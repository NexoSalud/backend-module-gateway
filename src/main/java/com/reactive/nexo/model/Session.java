package com.reactive.nexo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("session")
public class Session {

    @Id
    private Integer id;
    private String token;
    private Integer user_id;
    private String ip_address;
    private String useragent;
    private String created_at;
    private String expiration;
}
