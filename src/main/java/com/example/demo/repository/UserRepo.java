package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.User;

@Repository
public interface UserRepo extends MongoRepository<User, String>{

	public User findByName(String name);
	public List<User> findByAge(int age);
	
}
