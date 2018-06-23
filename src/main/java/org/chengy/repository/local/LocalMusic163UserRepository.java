package org.chengy.repository.local;

import org.chengy.model.Music163User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component("localUserRepository")
public interface LocalMusic163UserRepository extends MongoRepository<Music163User, String> {

    long countAllBySongRecordIsTrue();
}
