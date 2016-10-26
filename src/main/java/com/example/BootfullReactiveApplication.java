package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpServer;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@SpringBootApplication
public class BootfullReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootfullReactiveApplication.class, args);
	}

}

@Component
class PersonHandler{

	private PersonRepository personRepository;

	@Autowired
	public PersonHandler(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	Response<Flux<Person>> all(Request request) {
		Flux<Person> flux = Flux.fromStream(personRepository.all());
		return Response.ok().body(BodyInserters.fromPublisher(flux, Person.class));
	}

	Response<Mono<Person>> byId(Request request) {
		Optional<String> optional = request.pathVariable("id");
		return optional.map(id -> personRepository.findById(id))
				.map(person -> Mono.fromFuture(person))
				.map(mono -> Response.ok().body(BodyInserters.fromPublisher(mono, Person.class)))
				.orElseThrow(() -> new IllegalStateException("Ops"));
	}
}

@Component
class SimpleDataCRL implements CommandLineRunner {

	private final PersonRepository personRepository;

	@Bean
	RouterFunction<?> router(PersonHandler handler) {
		return RouterFunctions.route(RequestPredicates.GET("/persons"), handler::all)
				.and(RouterFunctions.route(RequestPredicates.GET("/persons/{id}"), handler::byId));
	}

	@Bean
	HttpServer server(RouterFunction<?> router) {
		HttpHandler handler = RouterFunctions.toHttpHandler(router);
		HttpServer httpServer = HttpServer.create(8000);
		httpServer.start(new ReactorHttpHandlerAdapter(handler));
		return httpServer;
	}


	@Autowired
	public SimpleDataCRL(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	@Override
	public void run(String... strings) throws Exception {
		Stream.of("Bruno Rocha", "Marli de Fatima", "Melina")
				.forEach(name -> personRepository.save(new Person(name, new Random().nextInt(100))));

		personRepository.all().forEach(System.out::println);
	}
}

interface PersonRepository extends MongoRepository<Person, String> {

	CompletableFuture<Person> findById(String id);

	@Query("{}")
	Stream<Person> all();
}

@Document
class Person {
	@Id
	private String id;
	private String nome;
	private Integer age;

	public Person(String nome, Integer age) {
		this.nome = nome;
		this.age = age;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "Person{" +
				"id='" + id + '\'' +
				", nome='" + nome + '\'' +
				", age='" + age + '\'' +
				'}';
	}
}
