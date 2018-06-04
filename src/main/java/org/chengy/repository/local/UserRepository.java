package org.chengy.repository.local;

import org.chengy.model.User;
import org.chengy.newmodel.Music163User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<Music163User, String> {
}
