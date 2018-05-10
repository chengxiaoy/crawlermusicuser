package org.chengy.repository.matcher;

import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.User;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

public class UserMatcherFactory {


    public static Example<User> music163BasicUserMatcher() {
        User user = new User();
        user.setCommunity(Music163ApiCons.communityName);
        Example<User> userExample = Example.of(user, ExampleMatcher.matching().withMatcher("community",
                match -> match.caseSensitive().exact()).withIgnorePaths("id", "gender").withIgnoreNullValues());
        return userExample;
    }
}
