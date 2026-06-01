package com.reactive.nexo.repository;

import com.reactive.nexo.model.Session;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SessionRepository extends ReactiveCrudRepository<Session, Integer> {
    @Query("select id,token,user_id,ip_address,useragent,created_at,expiration from session where token like $1")
    Flux<Session> findByToken(String token);   
}

