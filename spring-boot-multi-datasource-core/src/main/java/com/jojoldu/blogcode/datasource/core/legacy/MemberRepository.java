package com.jojoldu.blogcode.datasource.core.legacy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
