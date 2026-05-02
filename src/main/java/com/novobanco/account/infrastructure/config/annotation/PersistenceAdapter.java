package com.novobanco.account.infrastructure.config.annotation;

import org.springframework.stereotype.Repository;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repository
public @interface PersistenceAdapter {
}
