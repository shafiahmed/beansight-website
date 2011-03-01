package models;

import java.util.HashSet;
import java.util.Set;

public class Filter  {
	
	public enum FilterType {
		TRENDY,     // 0
		UPDATED           // 1
	}
	
	public Set<Category> 	categories;
	public Set<Tag>		tags;
	public Set<Language> 	languages;
	public String filterVote;
	public User user;
	
	public boolean favorites;
	
	public Filter() {
		categories = new HashSet<Category>();
		tags = new HashSet<Tag>();
		languages = new HashSet<Language>();
		favorites = false;
		filterVote = "all";
		user = null;
	}
	
	/**
	 * Generate a part of the where clause of a JPA query
	 * the insight must be called "i"
	 * example : "and c.id in ( '1','2' ) and i.lang.id in ('en')
	 * 
	 * I'm sorry, this is ugly but blame SQL.
	 */
	public String generateJPAQueryWhereClause(FilterType filterType) {
        String categoryIds = Category.listToIdString(this.categories);
        String languageIds = Language.listToIdString(this.languages);
		String whereQuery= "";
        if (!this.categories.isEmpty() || !this.languages.isEmpty()) {
			if (!this.categories.isEmpty()) {
				whereQuery += " and i.category.id in (" + categoryIds + ") ";
			}
			if (!this.languages.isEmpty()) {
				whereQuery += " and i.lang.id in (" + languageIds + ") ";
			}
		}
        if (filterType.equals(FilterType.TRENDY) && user != null) {
			if (filterVote.equals("voted")) {
				whereQuery += " and v.insight.id in (select distinct v.insight.id from Vote v where v.user.id = " + user.id + ")";
			} else if (filterVote.equals("notVoted")) {
				whereQuery += " and v.insight.id not in (select distinct v.insight.id from Vote v where v.user.id = " + user.id + ")";
			}
			whereQuery += " ";
        } else if (filterType.equals(FilterType.UPDATED) && user != null) {
			if (filterVote.equals("voted")) {
				whereQuery += " and i.id in (select distinct v.insight.id from Vote v where v.user.id = " + user.id + ")";
			} else if (filterVote.equals("notVoted")) {
				whereQuery += " and i.id not in (select distinct v.insight.id from Vote v where v.user.id = " + user.id + ")";
			}
        }
		return whereQuery;
	}
}
