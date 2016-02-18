package com.pmease.gitplex.core.gatekeeper;

import javax.validation.constraints.NotNull;

import org.eclipse.jgit.lib.ObjectId;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.wicket.editable.annotation.Editable;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.model.Depot;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Review;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.util.editable.UserChoice;

@SuppressWarnings("serial")
@Editable(order=200, icon="fa-user", category=GateKeeper.CATEGROY_CHECK_REVIEW, description=
		"This gate keeper will be passed if the commit is approved by specified user.")
public class IfApprovedBySpecifiedUser extends AbstractGateKeeper {

    private Long userId;

    @Editable(name="Select User Below")
    @UserChoice
    @NotNull
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public CheckResult doCheckRequest(PullRequest request) {
        User user = GitPlex.getInstance(Dao.class).load(User.class, getUserId());

        Review.Result result = user.checkReviewSince(request.getReferentialUpdate());
        if (result == null) {
            request.pickReviewers(Sets.newHashSet(user), 1);

            return pending(Lists.newArrayList("To be approved by " + user.getDisplayName() + "."));
        } else if (result == Review.Result.APPROVE) {
            return passed(Lists.newArrayList("Approved by " + user.getDisplayName() + "."));
        } else {
            return failed(Lists.newArrayList("Rejected by " + user.getDisplayName() + "."));
        }
    }

    @Override
    protected GateKeeper trim(Depot depot) {
        if (GitPlex.getInstance(Dao.class).get(User.class, getUserId()) == null)
            return null;
        else
            return this;
    }

    private CheckResult check(User user) {
		User approver = GitPlex.getInstance(Dao.class).load(User.class, userId);
        if (approver.getId().equals(user.getId())) {
        	return passed(Lists.newArrayList("Approved by " + approver.getName() + "."));
        } else {
        	return pending(Lists.newArrayList("Not approved by " + approver.getName() + ".")); 
        }
    }
    
	@Override
	protected CheckResult doCheckFile(User user, Depot depot, String branch, String file) {
		return check(user);
	}

	@Override
	protected CheckResult doCheckPush(User user, Depot depot, String refName, ObjectId oldCommit, ObjectId newCommit) {
		return check(user);
	}

}
