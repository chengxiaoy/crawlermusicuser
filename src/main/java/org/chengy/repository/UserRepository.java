package org.chengy.repository;

import org.chengy.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by nali on 2017/9/15.
 */
public interface UserRepository extends MongoRepository<User,String> {
	User findByCommunityIdAndCommunity(String communityId,String community);

	List<User> findByCommunityId(String communityId);
}
