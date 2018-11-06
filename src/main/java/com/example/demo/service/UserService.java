package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepo;

@Service
public class UserService {

	@Autowired
	private UserRepo userRepo;
	
	public User create(String name, int age) {
		return userRepo.save(new User (name, age));
	}
	
	public List<User> getAll(){
		return userRepo.findAll();
	}
	
	public User getByName(String name) {
		return userRepo.findByName(name);
	}
	
	public User update(String uname, int uage) {
		User u = userRepo.findByName(uname);
		u.setAge(uage);
		return userRepo.save(u);
	}
	
	public void deleteAll() {
		userRepo.deleteAll();
	}
	
	public void delete(String name) {
		User u = userRepo.findByName(name);
		userRepo.delete(u);
	}
	
}
