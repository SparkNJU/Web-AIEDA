package org.example.aiedabackend.dao;

import org.example.aiedabackend.po.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Account findByPhone(String phone);

}
