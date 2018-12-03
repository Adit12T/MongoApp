package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.User;
import com.example.demo.service.UserService;

@RestController
public class UserControl {

	@Autowired
	private UserService uService;
	
	@PostMapping("/create")
	public String create(@RequestParam String name, @RequestParam int age) {
		User u = uService.create(name, age);
		return u.toString();
	}
	
	@RequestMapping("/get")
	public User getUser(@RequestParam String name){
		return uService.getByName(name);
	}
	
	@RequestMapping("/getAll")
	public List<User> getAll(){
		return uService.getAll();
	}
	
	@RequestMapping("/update")
	public String update(@RequestParam String name, @RequestParam int age) {
		User u = uService.update(name, age);
		return u.toString();
	}
	
	@RequestMapping("/deleteAll")
	public String deleteAll(){
		uService.deleteAll();
		return "Deleted All Records";
	}
	
	@RequestMapping("/delete")
	public String delete(@RequestParam String name){
		uService.delete(name);
		return "Deleted" + name;
	}
	
}
