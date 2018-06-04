package org.chengy.repository.remote;

import org.chengy.newmodel.Music163User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface Music163UserRepository extends MongoRepository<Music163User, String> {

    long countAllBySongRecordIsTrue();
}
