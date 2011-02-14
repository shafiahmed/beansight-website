package models;

import helpers.FormatHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import models.Vote.Status;

import org.hibernate.annotations.Index;
import org.joda.time.DateTime;

import play.Logger;
import play.data.validation.MaxSize;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.db.jpa.Model;
import play.modules.search.Field;
import play.modules.search.Indexed;
import play.modules.search.Query;
import play.modules.search.Search;
import play.templates.JavaExtensions;
import exceptions.InsightWithSameUniqueIdAndEndDateAlreadyExistsException;


@Indexed
@Entity
public class Insight extends Model {

	/** unique id randomly generated by us to insure nobody can guess an insight id  */
	@Index(name = "INSIGHT_UNIQUE_ID_IXD")
	public String uniqueId;
	
	@ManyToOne
	@Required
	public User creator;

	/** the date this insight has been created by its creator */
	// TODO index this (unfortunately simply adding @Field transforms the date
	// in string)
	public Date creationDate;

	/** the date this insight is ending, defined by its creator */
	// TODO index this
	@Required
	public Date endDate;

	/** Content of the insight, a simple text describing the idea */
	@Field
	@Required
	@MinSize(5)
	@MaxSize(140)
	@Lob
	public String content;

	/** the language of the content of this insight */
	@ManyToOne
	public Language lang;
	
	/** Every vote of the current insight */
	@OneToMany(mappedBy = "insight", cascade = CascadeType.ALL)
	public List<Vote> votes;

	/** Every tag of the current insight */
	@ManyToMany(mappedBy = "insights", cascade = CascadeType.ALL)
	@Field
	public List<Tag> tags;

	@ManyToOne
	@Field
	@Required
	/** Category of this insight */
	public Category category;

	/** Users who follow the current insight */
	@ManyToMany(mappedBy = "followedInsights", cascade = CascadeType.ALL)
	public List<User> followers;

	/** Comments made to current insight */
	@OneToMany(mappedBy = "insight", cascade = CascadeType.ALL)
	@OrderBy("creationDate DESC")
	public List<Comment> comments;

	public boolean hidden;
	
	// model denormalization :
	// having to count agree and disagree each time you need to access an
	// insight is a performance killer
	/**
	 * current number of active "agree" votes (if someone changed his mind, it
	 * is not counted)
	 */
	public long agreeCount;
	/**
	 * current number of active "disagree" votes (if someone changed his mind,
	 * it is not counted)
	 */
	public long disagreeCount;
	/** the last time when someone voted for the insight */
	public Date lastUpdated;

	@OneToMany(mappedBy = "insight", cascade = CascadeType.ALL)
	@OrderBy(value = "trendDate")
	public List<Trend> trends;
	
	/** has this insight been validated by the ValidationJob ? */
	public boolean validated;
	/** True ? False ? Can't say ? Number between 0 and 1 representing the decided validation of this insight. */
	public double validationScore;

	/** Probability this insight has to occure before its endDate */
	public double occurenceScore;
	
	/**
	 * Create an insight
	 * 
	 * @param creator
	 * @param content
	 *            : content text of this insight
	 * @param endDate
	 *            : date this insight is supposed to end
	 * @param category
	 *            : the category of the insight
	 */
	public Insight(User creator, String content, Date endDate, Category category, Language lang) throws InsightWithSameUniqueIdAndEndDateAlreadyExistsException {
		this.uniqueId = JavaExtensions.slugify(content);
		// We insure that there's no insight having the same uniqueId
		// (This can happen since the uniqueId is the "slugified" of the insight content)
		Insight existingInsight = Insight.findByUniqueId(uniqueId);
		
		if (existingInsight != null) {
			if (existingInsight.endDate.getTime() == endDate.getTime()) {
				throw new InsightWithSameUniqueIdAndEndDateAlreadyExistsException();
			} else {
				// adding the date at the end of the uniqueId :
				this.uniqueId += "-" + FormatHelper.formatDate(endDate);
			}
		}
 
		this.creator = creator;
		this.creationDate = new Date();
		this.endDate = endDate;
		this.content = content;
		this.followers = new ArrayList<User>();
		this.comments = new ArrayList<Comment>();
		this.category = category;
		this.trends = new ArrayList<Trend>();
		this.lastUpdated = new Date();
		this.lang = lang;
		this.validated = false;
		this.validationScore = 0.5;
		this.occurenceScore = 0.5;
		this.hidden = false;
	}
	
	/**
	 * Tells if the current insight was created by the given User
	 * 
	 * @param user
	 * @return
	 */
	public boolean isCreator(User user) {
		if (creator.equals(user)) {
			return true;
		}
		return false;
	}

	/**
	 * a user adds tags from an input string.
	 * 
	 * @param tagLabelList
	 *            : list of tag labels separated by commas and spaces
	 * @param user
	 *            : the user adding the tag
	 */
	public void addTags(String tagLabelList, User user) {
		String[] labelArray = tagLabelList.split(",");

		for (int i = 0; i < labelArray.length; i++) {
			String label = labelArray[i].trim();
			this.addTag(label, user);
		}
	}

	/**
	 * Add a tag from a given label string, will check if tag already exists for
	 * this insight
	 * 
	 * @param label
	 *            : the label of the tag (will not be processed)
	 * @param user
	 *            : the user adding the tag
	 */
	private void addTag(String label, User user) {
		// TODO call here a method to normalize the label

		// check if this tag already exist for this insight
		boolean foundTag = false;
		if (this.tags != null) {
			for (Tag storedTag : this.tags) {
				if (storedTag.label.equalsIgnoreCase(label)) {
					storedTag.users.add(user);
					storedTag.save();
					foundTag = true;
					break;
				}
			}
		}
		// if not, check if this tag already exist on the website
		if (!foundTag) {
			Tag existTag = Tag.find("byLabel", label).first();
			if (existTag == null) {
				// if null, then create it.
				Tag newTag = new Tag(label, this, user);
				newTag.save();
			} else {
				// if found, then associate with this insight and this user.
				existTag.insights.add(this);
				existTag.users.add(user);
				existTag.save();
			}
		}
	}

	/**
	 * Add a comment to the current insight
	 * 
	 * @param content
	 * @param user
	 */
	public Comment addComment(String content, User user) {
		Comment comment = new Comment(user, this, content);
		comment.save();
		return comment;
	}

	/**
	 * get the list of the n last active votes for this Insight
	 * 
	 * @param n
	 *            : the maximum number of votes to return
	 * @return: the list of n most recent active votes
	 */
	public List<Vote> getLastVotes(int n) {
		return Vote.find(
				"select v from Vote v " + "join v.insight i "
						+ "where v.status = :status and i.id=:insightId "
						+ "order by v.creationDate DESC").bind("status",
				Status.ACTIVE).bind("insightId", this.id).fetch(n);
	}

	/**
	 * Performs a search action
	 * 
	 * @param query : the search query
	 * @param from : index of the first item to be returned
	 * @param number : number of items to return
	 * @return an object containing the result list and the total result
	 */
	public static InsightResult search(String query, int from, int number, Filter filter) {
		Category cat = null;
		if( ! filter.categories.isEmpty()) {
			for(Category catego : filter.categories) {
				cat = catego;
			}
		}
		
		// TODO Steren : this query string construction is temporary, we should better handle this
		String fullQueryString = "(content:" + query + " OR tags:" + query + ") ";
		if (cat != null) {
			fullQueryString += " AND category:" + cat.id;
		}
		Logger.info( "SEARCH:" + fullQueryString );

		Query q = Search.search(fullQueryString, Insight.class);

		// create the result object
		InsightResult result = new InsightResult();
		result.count = q.count();

		// restrict to a sub group
		q.page(from, number);

		result.results = q.fetch();

		return result;
	}

	/**
	 * @param from : index of the first item to be returned
	 * @param number : number of items to return
	 */
	public static InsightResult findLatest(int from, int number, Filter filter) {
        String query = "select i from Insight i where i.hidden is false "
				        + filter.generateJPAQueryWhereClause()
						+ " order by lastUpdated DESC";

		InsightResult result = new InsightResult();
		// TODO : return total number using count ?
		// result.count = Insight.count(query);

		result.results = Insight.find(query).from(from).fetch(number);

		return result;
	}
	
	/**
	 * Return the most voted insights from the previous 24 hours
	 * @param from
	 * @param length
	 * @param filter 
	 * @return
	 */
	public static InsightResult findTrending(int from, int length, Filter filter) {
		InsightResult result = new InsightResult();
		String query = "select v.insight from Vote v "
						+ "join v.insight i "
						+ "where i.hidden is false "
						+ "and v.creationDate > ? " // Of course, do not check the status of the vote.
						+ filter.generateJPAQueryWhereClause()
						+ "group by v.insight.id "
						+ "order by count(v) desc";
		
		result.results = Insight.find(query, new DateTime().minusHours(24).toDate() ).from(from).fetch(length);	
		return result;
	}

	/**
	 * @param page : the page number to start from
	 * @param number : number of items per page
	 */
	public static List<Insight> findNotValidatedAndEndDateOver(int page, int number) {
		List<Insight> insights = Insight.find("hidden is false and validated is false and endDate < ?", new Date()).fetch(page, number);
		return insights;
	}
	
	/**
	 * @param page : the page number to start from
	 * @param number : number of items per page
	 */
	public static List<Insight> findEndDateNotOver(int page, int number) {
		List<Insight> insights = Insight.find("hidden is false and endDate > ?", new Date()).fetch(page, number);
		return insights;
	}
	
	/**
	 * Create a snapshot of this insight state, store it in a Trend
	 */
	public void createTrendSnapshot() {
		this.trends.add(new Trend(new Date(), this, this.agreeCount, this.disagreeCount));
	}
	
    public long getTrendCount() {
    	return Trend.count("insight", this);
   }
	
    /**
     * 
     * 
     * @param horizontalDefinition Number of horizontal value that will be used to create the charts
     * @return
     */
    public List<Double> getAgreeRatioTrends(long horizontalDefinition) {
        long trendsCount = this.getTrendCount();
                
        List<Double> agreeTrends;
        if (trendsCount <= horizontalDefinition) {
            agreeTrends = find("select t.agreeRatio from Trend t join t.insight i where i.id = :insightId order by t.trendDate").bind("insightId", this.id)
                    .fetch();
        } else {
        	// FIXME : be careful, the "-2" make it possible for the value to be 0
        	long incrementSize = (trendsCount - 2) / horizontalDefinition;
            List<Long> indexList = new ArrayList<Long>((int)horizontalDefinition);
            for (int i = 1 ; i<horizontalDefinition ; i++) {
                indexList.add(i * incrementSize + 1);
            }
            
            agreeTrends = find(
                    "select t.agreeRatio from Trend t join t.insight i where i.id = :insightId and t.relativeIndex in (:indexList) order by t.trendDate")
                    .bind("insightId", this.id).bind("indexList", indexList).fetch();
        }

        return agreeTrends;
    }
    
    /**
     * Get an insight using its uniqueId
     * @param uniqueId
     * @return
     */
    public static Insight findByUniqueId(String uniqueId) {
    	return find("select i from Insight i where i.uniqueId = :uniqueId").bind("uniqueId", uniqueId).first();
    }
    
    public List<Comment> findDisplayableComments() {
    	return Comment.find("select c from Comment c where c.insight.id = :insightId and c.hidden is false order by c.creationDate").bind("insightId", this.id).fetch();
    }
    
	public static class InsightResult {
		/** The asked insights */
		public List<Insight> results;
		/** the total number of results */
		public long count;
	}

	public String toString() {
		return content;
	}
}
