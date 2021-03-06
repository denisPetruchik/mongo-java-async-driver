package jp.ameba.mongo;

import java.io.IOException;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientTest {

	private MongoClient client;
	
	@Before
	public void before() throws IOException {
		client = new MongoClient();
		client.setHosts("127.0.0.1:27017");
		client.awaitOpen();
		Assert.assertTrue(client.isOpen());
	}
	
	@After
	public void after() throws IOException, InterruptedException {
		client.close();
		Thread.sleep(100L);
		Assert.assertFalse(client.isOpen());
	}
	
	@Test
	public void testDrop() throws Exception {
		
		MongoCollection collection = client.getCollection("test", "client");
		collection.upsert(new BasicBSONObject("_id", 1), new BasicBSONObject("_id", 1).append("name", "name-1"));
		collection.drop();
		
		Assert.assertEquals(0, collection.count());
				
	}
	
	@Test
	public void testInsert() throws Exception {
		
		MongoCollection collection = client.getCollection("test", "client");
		
		collection.remove(new BasicBSONObject());
		Assert.assertEquals(0, collection.count());
		
		for (int i = 0; i < 100; i++) {
			BSONObject doc = new BasicBSONObject()
			.append("_id", i)
			.append("name", "name-" + i)
			.append("value", "value-" + i);
			collection.insert(doc);
		}
		
		Assert.assertEquals(100, collection.count());
		
		int count = 0;
		MongoCursor cursor = collection.cursor();
		for (BSONObject object : cursor) {
			int i = (Integer) object.get("_id");
			Assert.assertEquals("name-" + i, object.get("name"));
			Assert.assertEquals("value-" + i, object.get("value"));
			count++;
		}
		cursor.close();
		
		Assert.assertEquals(100, count);
		
	}
	
	@Test
	public void testUpdate() throws Exception {
		
		MongoCollection collection = client.getCollection("test", "client");
		collection.remove(new BasicBSONObject());
		Assert.assertEquals(0, collection.count());
		
		for (int i = 0; i < 100; i++) {
			BSONObject doc = new BasicBSONObject()
			.append("_id", i)
			.append("name", "name-" + i)
			.append("value", "value-" + i);
			collection.insert(doc);
		}
		
		Assert.assertEquals(100, collection.count());
		
		for (int i = 0; i < 100; i++) {
			collection.update(
					new BasicBSONObject("_id", i),
					new BasicBSONObject("$set", new BasicBSONObject("name", "updated-name-" + i)));
		}
		
		Assert.assertEquals(100, collection.count());
		
		int count = 0;
		
		for (BSONObject object : collection.cursor()) {
			int i = (Integer) object.get("_id");
			Assert.assertEquals("updated-name-" + i, object.get("name"));
			Assert.assertEquals("value-" + i, object.get("value"));
			count++;
		}
		
		Assert.assertEquals(100, count);
		
		for (int i = 0; i < 100; i++) {
			collection.update(
					new BasicBSONObject("_id", i),
					new BasicBSONObject("_id", i)
					.append("name", "new-name-" + i)
			);
		}

		count = 0;
		
		for (BSONObject object : collection.cursor()) {
			int i = (Integer) object.get("_id");
			Assert.assertEquals("new-name-" + i, object.get("name"));
			Assert.assertFalse(object.containsField("value"));
			count++;
		}
		
		Assert.assertEquals(100, count);
		
		for (int i = 50; i < 150; i++) {
			collection.upsert(
					new BasicBSONObject("_id", i),
					new BasicBSONObject("_id", i)
					.append("name", "upsert-name-" + i)
					.append("value", "upsert-value-" + i)
			);
		}
		
		Assert.assertEquals(150, collection.count());
		
		count = 0;
		
		for (BSONObject object : collection.cursor()) {
			int i = (Integer) object.get("_id");
			if (i < 50) {
				Assert.assertEquals("new-name-" + i, object.get("name"));
				Assert.assertFalse(object.containsField("value"));
			} else {
				Assert.assertEquals("upsert-name-" + i, object.get("name"));
				Assert.assertEquals("upsert-value-" + i, object.get("value"));
			}
			count++;
		}
		
		Assert.assertEquals(150, count);
	}
	
	@Test
	public void testRemove() throws Exception {
		
		MongoCollection collection = client.getCollection("test", "client");
		collection.remove(new BasicBSONObject());
		Assert.assertEquals(0, collection.count());
		
		for (int i = 0; i < 100; i++) {
			BSONObject doc = new BasicBSONObject()
			.append("_id", i)
			.append("name", "name-" + i)
			.append("value", "value-" + i);
			collection.insert(doc);
		}
		
		Assert.assertEquals(100, collection.count());
		
		for (int i = 0; i < 100; i++) {
			Assert.assertEquals(1, collection.count(new BasicBSONObject("_id", i)));
			collection.remove(new BasicBSONObject("_id", i));
			Assert.assertEquals(0, collection.count(new BasicBSONObject("_id", i)));
		}
		
		Assert.assertEquals(0, collection.count());
	}
	
	@Test
	public void testFindAndModify() throws Exception {
		
		MongoCollection collection = client.getCollection("test", "client");
		collection.drop();
		
		for (int i = 0; i < 100; i++) {
			BSONObject doc = new BasicBSONObject()
			.append("_id", i)
			.append("name", "name-" + i);
			collection.insert(doc);
		}
		
		
		BSONObject result = 
			collection.findAndModify()
			.query(new BasicBSONObject("_id", 20))
			.update(new BasicBSONObject("_id", 20).append("name", "name-updated-" + 20))
			.getnew()
			.execute();
		
		Assert.assertNotNull(result);
		Assert.assertEquals(20, result.get("_id"));
		Assert.assertEquals("name-updated-20", result.get("name"));
		
		
	}
	
	@Test
	public void testIndex() throws Exception {
		
		MongoCollection collection = client.getCollection("test", "client");
		collection.drop();
		
		MongoCollection indexCollection = client.getCollection("test", "system.indexes");
		long currentIndexCount = indexCollection.count();
		
		for (int i = 0; i < 100; i++) {
			BSONObject doc = new BasicBSONObject()
			.append("_id", i)
			.append("name", "name-" + i);
			collection.insert(doc);
		}
		
		Assert.assertEquals(100, collection.count());
		
		collection.createIndex("name_", new BasicBSONObject("name", 1), new BasicBSONObject());
		
		Assert.assertEquals(currentIndexCount+2, indexCollection.count());
		
		
	}
}
