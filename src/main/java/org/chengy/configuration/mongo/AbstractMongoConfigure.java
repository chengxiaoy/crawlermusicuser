package org.chengy.configuration.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

public abstract class AbstractMongoConfigure {
	private String host, database;
	private int port;

	/*
	 * Method that creates MongoDbFactory
	 * Common to both of the MongoDb connections
	 */
	public MongoDbFactory mongoDbFactory() throws Exception {


		MongoClientOptions mongoClientOptions = MongoClientOptions.builder().minConnectionsPerHost(20).connectionsPerHost(1000).build();
		return new SimpleMongoDbFactory(new MongoClient(new ServerAddress(host, port), mongoClientOptions), database);
	}

	/*
	 * Factory method to create the MongoTemplate
	 */
	abstract public MongoTemplate getMongoTemplate() throws Exception;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
