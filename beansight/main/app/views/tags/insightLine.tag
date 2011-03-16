*{ Display the info for a given insight  }*
*{ @param insigth: the insight  }*
*{ @param targetUser: a specific user that we want to display the vote, leave null if no specific user }*
%{ 
    String voteTargetUserClass = "";
    if(_targetUser != null) {
      voteTargetUser = models.Vote.findLastVoteByUserAndInsight(_targetUser.id, _insight.uniqueId);
      if(voteTargetUser && voteTargetUser.state.equals(models.Vote.State.AGREE)) {
        voteTargetUserClass = "agreeaction";
      } else if(voteTargetUser && voteTargetUser.state.equals(models.Vote.State.DISAGREE)) {
        voteTargetUserClass = "disagreeaction";
      }
    }
/}%
#{insightContainer insight:_insight}
<div class="item-insight">
    <div class="content-insight">
        #{agree-disagreeWidget insight:_insight/}
        <a href="@{Application.showInsight(_insight.uniqueId)}" class="permalink #{if _targetUser != null}leftbusy#{/if}">
            <p class="date-insight" style="color: #A1A5A6">${_insight.endDate.in(true)}, </p>
            <h3>${_insight.content}</h3> 
            #{if _targetUser != null}
            <span class="target-user">
                <span class="avatar-user"><img src="@{Application.showAvatarSmall(_targetUser.userName, _targetUser.avatarHashCode())}" alt=""/></span>
                <span class="vote-user ${voteTargetUserClass}">Vote ${_targetUser.userName}</span>
            </span>
            #{/if}
            #{if _insight.comments.size() > 0}
                <p class="date-insight" style="float: right; clear:both;">(${_insight.comments.size()} &{'insights.comments', _insight.comments.size().pluralize2()})</p>
            #{/if}
        </a>
        <hr class="clear"/>
    </div>
</div>
#{/insightContainer}
