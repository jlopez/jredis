/*
 *   Copyright 2009 Joubin Houshyar
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *    
 *   http://www.apache.org/licenses/LICENSE-2.0
 *    
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.jredis.ri.alphazero;

import static org.jredis.ri.alphazero.support.DefaultCodec.decode;
import static org.jredis.ri.alphazero.support.DefaultCodec.toLong;
import static org.jredis.ri.alphazero.support.DefaultCodec.toStr;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import java.util.List;
import java.util.Map;
import org.jredis.JRedis;
import org.jredis.RedisException;
import org.jredis.RedisInfo;
import org.jredis.RedisType;
import org.jredis.protocol.Command;
import org.jredis.ri.JRedisTestSuiteBase;
import org.jredis.ri.alphazero.support.DefaultCodec;
import org.jredis.ri.alphazero.support.Log;
import org.testng.annotations.Test;

/**
 * This class is abstract and it is to remain abstract.
 * It provides the comprehensive set of tests of all {@link JRedis} methods.
 */

//TODO: get rid of NG in class name

public abstract class JRedisProviderTestsBase extends JRedisTestSuiteBase <JRedis>{

	// ------------------------------------------------------------------------
	// Properties
	// ------------------------------------------------------------------------

	/** JRedis Command being tested -- for log info */
	private String cmd;
	
	// ------------------------------------------------------------------------
	// The Tests
	// ========================================================= JRedis =======
	/**
	 * We define and run provider agnostic tests here.  This means we run a set
	 * of JRedis interface method tests that every connected JRedis implementation
	 * should be able to support. 
	 * 
	 * The following commands are omitted:
	 * 1 - QUIT: since we may be testing a multi-connection provider
	 * 2 - SHUTDOWN: for the same reason as QUIT 
	 * 3 - MOVE and SELECT
	 */
	// ------------------------------------------------------------------------

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#auth(java.lang.String)}.
	 */
	@Test
	public void testElicitErrors() {
		Log.log("TEST: Elicit errors");
		try {
			provider.flushdb();
			
			String key = keys.get(0);
			provider.set(key, smallData);
			boolean expectedError;
			
			// -- commands returning status response 
			expectedError = false;
			try {
				Log.log("Expecting an operation against key holding the wrong kind of value ERROR..");
				provider.sadd(key, dataList.get(0)); 
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "should have raised an exception but did not");
			
			// -- commands returning value response 
			expectedError = false;
			try {
				Log.log("Expecting an operation against key holding the wrong kind of value ERROR..");
				provider.scard(key); 
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "should have raised an exception but did not");
			
			// -- commands returning bulk response
			expectedError = false;
			try {
				Log.log("Expecting an operation against key holding the wrong kind of value ERROR..");
				provider.lpop(key); 
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "should have raised an exception but did not");
			
			
			// -- commands returning multi-bulk response 
			expectedError = false;
			try {
				Log.log("Expecting an operation against key holding the wrong kind of value ERROR..");
				provider.smembers(key); 
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "should have raised an exception but did not");
			
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	
//	/**
//	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#auth(java.lang.String)}.
//	 */
//	@Test
//	public void testAuth() {
//		test = Command.AUTH.code;
//		Log.log("TEST: %s command", test);
//		try {
//			jredis.auth(password);
//		} 
//		catch (RedisException e) {
//			fail(test + " with password: " + password, e);
//		}
//	}
	
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#ping()}.
	 */
	@Test
	public void testPing() {
		cmd = Command.PING.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.ping();
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	/**
	 * Tests:
	 * <li>Test method for {@link org.jredis.ri.alphazero.JRedisSupport#exists(java.lang.String)}.
	 * <li>Test method for {@link org.jredis.ri.alphazero.JRedisSupport#expire(java.lang.String, int)}.
	 * <li>Test method for {@link org.jredis.ri.alphazero.JRedisSupport#ttl (java.lang.String)}.
	 */
	@Test
	public void testExists_Expire_TTL() {
		cmd = Command.EXISTS.code + " | " + Command.EXPIRE.code + " | " + Command.TTL.code;
		Log.log("TEST: %s command(s)", cmd);
		try {
			provider.flushdb();
			assertTrue(provider.dbsize() == 0);
			
			String keyToExpire = "expire-me";
			String keyToKeep = "keep-me";
			
			provider.set(keyToKeep, "master");
			provider.set(keyToExpire, System.currentTimeMillis());
			assertTrue (provider.exists(keyToExpire));
			
			Log.log("TEST: %s with expire time of %d", Command.EXPIRE, expire_secs);
			provider.expire(keyToExpire, expire_secs);
			assertTrue (provider.exists(keyToExpire));
			
			assertTrue (provider.ttl(keyToExpire) > 0, "key to expire ttl is less than zero");
			
			// NOTE: IT SIMPLY WON'T WORK WITHOUT GIVING REDIS A CHANCE
			// could be network latency, or whatever, but the expire command is NOT
			// that precise
			
			Thread.sleep(500);
			assertTrue (provider.exists(keyToExpire));
			
			Thread.sleep(this.expire_wait_millisecs);
			assertFalse (provider.exists(keyToExpire));
			assertTrue (provider.ttl(keyToExpire) == -1, "expired key ttl is not -1");
			assertTrue (provider.ttl(keyToKeep) == -1, "key to keep ttl is not -1");
			
			
		} 
		catch (RedisException e) {
			fail(cmd + " with password: " + password, e);
		}
		catch (InterruptedException e) {
			fail (cmd + "thread was interrupted and test did not conclude" + e.getLocalizedMessage());
		}
	}

// CANT test this without risking hosing the user's DBs
// TODO: use a flag
//	/**
//	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#flushall()}.
//	 */
//	@Test
//	public void testFlushall() {
//		fail("Not yet implemented");
//	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#flushdb()}.
	 */
	@Test
	public void testSelectAndFlushdb() {
		cmd = 
			Command.SELECT.code + " | " + 
			Command.FLUSHDB.code + " | " +
			Command.SET.code + " | " +
			Command.EXISTS.code + " | " +
			Command.FLUSHDB.code + " | " +
			Command.KEYS.code;
			
		Log.log("TEST: %s commands", cmd);
		try {
			key = "woof";
			provider.flushdb();
			provider.set(key, "meow");
			assertTrue (provider.exists(key));
			provider.flushdb();
			assertTrue(provider.keys().size()==0);
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#rename(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRename() {
		cmd = Command.RENAME.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String newkey = null;
			byte[] value = dataList.get(0);
			key = getRandomAsciiString (random.nextInt(24)+2);
			newkey = getRandomAsciiString (random.nextInt(24)+2);
			
			provider.set (key, value);
			assertEquals(value, provider.get(key));
			provider.rename (key, newkey);
			assertEquals(value, provider.get(newkey));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#renamenx(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRenamenx() {
		cmd = Command.RENAMENX.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			provider.set (keys.get(0), dataList.get(0));
			assertEquals(dataList.get(0), provider.get(keys.get(0)));

			// should work
			assertTrue(provider.renamenx (keys.get(0), keys.get(2)));
			assertEquals(dataList.get(0), provider.get(keys.get(2)));
			
			provider.flushdb();
			
			// set key1
			provider.set (keys.get(1), dataList.get(1));
			assertEquals(dataList.get(1), provider.get(keys.get(1)));
			
			// set key2
			provider.set (keys.get(2), dataList.get(2));
			assertEquals(dataList.get(2), provider.get(keys.get(2)));
			
			// rename key1 to key 2 
			// should not
			assertFalse(provider.renamenx (keys.get(1), keys.get(2)));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#save()}.
	 */
	@Test
	public void testSaveAndLastSave() {
		cmd = Command.SAVE.code + " | " + Command.LASTSAVE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.save();
			long when = provider.lastsave();
			Thread.sleep (this.expire_wait_millisecs);
			provider.save();
			long when2 = provider.lastsave();
			assertTrue(when != when2);
		} 
		catch (RedisException e) { 
			if(e.getLocalizedMessage().indexOf("background save in progress") != -1){
				Log.problem ("** NOTE ** Redis background save in progress prevented effective test of SAVE and LASTSAVE.");
			}
			else 
				fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); 
		}
		catch (InterruptedException e) {
			fail ("thread was interrupted and test did not conclude" + e.getLocalizedMessage());
		}
	}
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#bgsave()}.
	 */
	@Test
	public void testBgsave() {
		cmd = Command.BGSAVE.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			// TODO: what's a meaningful test for this besides asserting command works?
			provider.bgsave();
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, byte[])}.
	 */
	@Test
	public void testSetStringByteArray() {
		cmd = Command.SET.code + " | " + Command.SETNX.code + " byte[] | " + Command.GET;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.set(keys.get(keys.size()-1), emptyBytes);
			assertEquals(provider.get(keys.get(keys.size()-1)), emptyBytes, "set and get results for empty byte[]");
			
			provider.set(keys.get(0), dataList.get(0));
			assertEquals(dataList.get(0), provider.get(keys.get(0)), "data and get results");
			
			assertTrue(provider.setnx(keys.get(1), dataList.get(1)), "set key");
			assertNotNull(provider.get(keys.get(1)));
			assertFalse(provider.setnx(keys.get(1), dataList.get(2)), "key was already set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testSetStringString() {
		cmd = Command.SET.code + " | " + Command.SETNX.code + " String | " + Command.GET;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.set(keys.get(keys.size()-1), emptyString);
			assertEquals(toStr(provider.get(keys.get(keys.size()-1))), emptyString, "set and get results for empty String");
			
			provider.set(keys.get(0), stringList.get(0));
			assertEquals(stringList.get(0), toStr(provider.get(keys.get(0))), "string and get results");
			
			assertTrue(provider.setnx(keys.get(1), stringList.get(1)), "set key");
			assertNotNull(provider.get(keys.get(1)));
			assertFalse(provider.setnx(keys.get(1), stringList.get(2)), "key was already set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, java.lang.Number)}.
	 */
	@Test
	public void testSetStringNumber() {
		cmd = Command.SET.code + " | " + Command.SETNX.code + " Long | " + Command.GET;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.set(keys.get(0), longList.get(0));
			assertTrue(longList.get(0).equals(toLong(provider.get(keys.get(0)))), "long and get results");
			
			assertTrue(provider.setnx(keys.get(1), longList.get(1)), "set key");
			assertNotNull(provider.get(keys.get(1)));
			assertFalse(provider.setnx(keys.get(1), longList.get(2)), "key was already set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, java.io.Serializable)}.
	 */
	@Test
	public void testSetStringT() {
		cmd = Command.SET.code + " | " + Command.SETNX.code + " Java Object | " + Command.GET;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.set(keys.get(0), objectList.get(0));
			assertTrue(objectList.get(0).equals(decode(provider.get(keys.get(0)))), "object and get results");
			
			assertTrue(provider.setnx(keys.get(1), objectList.get(1)), "set key");
			assertNotNull(provider.get(keys.get(1)));
			assertFalse(provider.setnx(keys.get(1), objectList.get(2)), "key was already set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	
	
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, byte[])}.
	 */
	@Test
	public void testGetSetStringByteArray() {
		cmd = Command.SET.code + " | " + Command.GETSET.code + " byte[] ";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.set(keys.get(0), dataList.get(0));
			assertEquals(dataList.get(0), provider.get(keys.get(0)), "data and get results");
			
			assertEquals (provider.getset(keys.get(0), dataList.get(1)), dataList.get(0), "getset key");
			
			assertEquals (provider.get(keys.get(1)), null, "non existent key should be null");
			assertEquals (provider.getset(keys.get(1), dataList.get(1)), null, "getset on null key should be null");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, java.lang.String)}.
	 */
//	@Test
//	public void testGetSetStringString() {
//		test = Command.SET.code + " | " + Command.GETSET.code + " String ";
//		Log.log("TEST: %s command", test);
//		try {
//			jredis.flushdb();
//			
//			jredis.set(keys.get(0), stringList.get(0));
//			assertEquals(stringList.get(0), toStr(jredis.get(keys.get(0))), "string and get results");
//			
//			assertTrue(jredis.setnx(keys.get(1), stringList.get(1)), "set key");
//			assertNotNull(jredis.get(keys.get(1)));
//			assertFalse(jredis.setnx(keys.get(1), stringList.get(2)), "key was already set");
//		} 
//		catch (RedisException e) { fail(test + " ERROR => " + e.getLocalizedMessage(), e); }
//	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, java.lang.Number)}.
	 */
//	@Test
//	public void testGetSetStringNumber() {
//		test = Command.SET.code + " | " + Command.GETSET.code + " Number ";
//		Log.log("TEST: %s command", test);
//		try {
//			jredis.flushdb();
//			
//			jredis.set(keys.get(0), longList.get(0));
//			assertTrue(longList.get(0).equals(toLong(jredis.get(keys.get(0)))), "long and get results");
//			
//			assertTrue(jredis.setnx(keys.get(1), longList.get(1)), "set key");
//			assertNotNull(jredis.get(keys.get(1)));
//			assertFalse(jredis.setnx(keys.get(1), longList.get(2)), "key was already set");
//		} 
//		catch (RedisException e) { fail(test + " ERROR => " + e.getLocalizedMessage(), e); }
//	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#set(java.lang.String, java.io.Serializable)}.
	 */
//	@Test
//	public void testGetSetStringT() {
//		test = Command.SET.code + " | " + Command.GETSET.code + " Java Object ";
//		Log.log("TEST: %s command", test);
//		try {
//			jredis.flushdb();
//			
//			jredis.set(keys.get(0), objectList.get(0));
//			assertTrue(objectList.get(0).equals(decode(jredis.get(keys.get(0)))), "object and get results");
//			
//			assertTrue(jredis.setnx(keys.get(1), objectList.get(1)), "set key");
//			assertNotNull(jredis.get(keys.get(1)));
//			assertFalse(jredis.setnx(keys.get(1), objectList.get(2)), "key was already set");
//		} 
//		catch (RedisException e) { fail(test + " ERROR => " + e.getLocalizedMessage(), e); }
//	}

	
	

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#incr(java.lang.String)}.
	 */
	@Test
	public void testIncrAndDecr() {
		cmd = Command.INCR.code + " | " + Command.DECR.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			long cntr = 0;
			String cntr_key = keys.get(0);
			
			for(int i = 1; i<MEDIUM_CNT; i++){
				cntr = provider.incr(cntr_key);
				assertEquals(i, cntr);
			}
			
			for(long i=cntr-1; i>=0; i--){
				cntr = provider.decr(cntr_key);
				assertEquals(i, cntr);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#incrby(java.lang.String, int)}.
	 */
	@Test
	public void testIncrbyAndDecrby() {
		cmd = Command.INCRBY.code + " |" + Command.DECRBY.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			long cntr = 0;
			String cntr_key = keys.get(0);

			for(long i = 1; i<MEDIUM_CNT; i++){
				cntr = provider.incrby(cntr_key, 10);
				assertEquals(i*10, cntr);
			}
			
			provider.set(cntr_key, 0);
			assertTrue(0 == toLong(provider.get(cntr_key)));
			for(long i = 1; i<MEDIUM_CNT; i++){
				cntr = provider.decrby(cntr_key, 10);
				assertEquals(i*-10, cntr);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#del(java.lang.String)}.
	 */
	@Test
	public void testDel() {
		cmd = Command.DEL.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String key = this.keys.get(0);
			provider.set (key, dataList.get(0));
			assertTrue (provider.exists(key));
			
			provider.del(key);
			assertFalse (provider.exists(key));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#mget(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testMget() {
		cmd = Command.MGET.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			for(int i=0; i<SMALL_CNT; i++){
				provider.set (keys.get(i), dataList.get(i));
			}
			
			List<byte[]>  values = null;
			values = provider.mget(keys.get(0)); 
			assertEquals(values.size(), 1, "one value expected");
			for(int i=0; i<1; i++)
				assertEquals(values.get(i), dataList.get(i));
			
			values = provider.mget(keys.get(0), keys.get(1)); 
			assertEquals(values.size(), 2, "2 values expected");
			for(int i=0; i<2; i++)
				assertEquals(values.get(i), dataList.get(i));
			
			values = provider.mget(keys.get(0), keys.get(1), keys.get(2)); 
			assertEquals(values.size(), 3, "3 values expected");
			for(int i=0; i<3; i++)
				assertEquals(values.get(i), dataList.get(i));
			
			values = provider.mget("foo", "bar", "paz"); 
			assertEquals(values.size(), 3, "3 values expected");
			for(int i=0; i<3; i++)
				assertEquals(values.get(i), null, "nonexistent key value in list should be null");
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	
	/**************** LIST COMMANDS ******************************/

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#rpush(java.lang.String, byte[])}.
	 */
	@Test
	public void testRpushStringByteArray() {
		cmd = Command.RPUSH.code + " byte[] | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.rpush(listkey, dataList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				assertEquals (dataList.get(i), range.get(i), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lpush(java.lang.String, byte[])}.
	 */
	@Test
	public void testLpushStringByteArray() {
		cmd = Command.LPUSH.code + " byte[] | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.lpush(listkey, dataList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				int r = SMALL_CNT - i - 1;
				assertEquals (dataList.get(i), range.get(r), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}


	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#rpush(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRpushStringString() {
		cmd = Command.RPUSH.code + " String | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.rpush(listkey, stringList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				assertEquals (stringList.get(i), toStr(range.get(i)), "range and reference list differ at i: " + i);
			}
			List<String>  strRange = toStr(range);
			for(int i=0; i<SMALL_CNT; i++){
				assertEquals (stringList.get(i), strRange.get(i), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lpush(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testLpushStringString() {
		cmd = Command.LPUSH.code + " String | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.lpush(listkey, stringList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				int r = SMALL_CNT - i - 1;
				assertEquals (stringList.get(i), toStr(range.get(r)), "range and reference list differ at i: " + i);
			}
			List<String>  strRange = toStr(range);
			for(int i=0; i<SMALL_CNT; i++){
				int r = SMALL_CNT - i - 1;
				assertEquals (stringList.get(i), strRange.get(r), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#rpush(java.lang.String, java.lang.Number)}.
	 */
	@Test
	public void testRpushStringNumber() {
		cmd = Command.RPUSH.code + " Number | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.rpush(listkey, this.longList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				assertTrue (longList.get(i).equals(toLong(range.get(i))), "range and reference list differ at i: " + i);
			}
			List<Long>  longRange = toLong(range);
			for(int i=0; i<SMALL_CNT; i++){
				assertEquals (longList.get(i), longRange.get(i), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lpush(java.lang.String, java.lang.Number)}.
	 */
	@Test
	public void testLpushStringNumber() {
		cmd = Command.LPUSH.code + " Number | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.lpush(listkey, this.longList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				int r = SMALL_CNT - i - 1;
				assertTrue (longList.get(i).equals(toLong(range.get(r))), "range and reference list differ at i: " + i);
			}
			List<Long>  longRange = toLong(range);
			for(int i=0; i<SMALL_CNT; i++){
				int r = SMALL_CNT - i - 1;
				assertEquals (longList.get(i), longRange.get(r), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#rpush(java.lang.String, java.io.Serializable)}.
	 */
	@Test
	public void testRpushStringT() {
		cmd = Command.RPUSH.code + " Java Object | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.rpush(listkey, this.objectList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				assertTrue (objectList.get(i).equals(decode(range.get(i))), "range and reference list differ at i: " + i);
			}
			List<TestBean>  objRange = decode(range);
			for(int i=0; i<SMALL_CNT; i++){
				assertEquals (objectList.get(i), objRange.get(i), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lpush(java.lang.String, java.io.Serializable)}.
	 */
	@Test
	public void testLpushStringT() {
		cmd = Command.LPUSH.code + " Java Object | " + Command.LLEN + " | " + Command.LRANGE;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			for(int i=0; i<SMALL_CNT; i++){
				provider.lpush(listkey, this.objectList.get(i));
			}
			// use LLEN: size should be small count
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// use LRANGE 0 cnt: equal size and data should be same in order
			List<byte[]>  range = provider.lrange(listkey, 0, SMALL_CNT);
			assertTrue(range.size()==SMALL_CNT, "range size after RPUSH is wrong");
			for(int i=0; i<SMALL_CNT; i++){
				int r = SMALL_CNT - i - 1;
				assertTrue (objectList.get(i).equals(decode(range.get(r))), "range and reference list differ at i: " + i);
			}
			List<TestBean>  objRange = decode(range);
			for(int i=0; i<SMALL_CNT; i++){
				int r = SMALL_CNT - i - 1;
				assertEquals (objectList.get(i), objRange.get(r), "range and reference list differ at i: " + i);
			}
		} 
		catch (RedisException e) {
			fail(cmd + " ERROR => " + e.getLocalizedMessage(), e);
		}
	}
	
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#ltrim(java.lang.String, long, long)}.
	 */
	@Test
	public void testLtrim() {
		cmd = Command.LTRIM.code + " | " + Command.LLEN.code + " | " + Command.LRANGE.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			// prep a small list
			String listkey = keys.get(0);
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, dataList.get(i)); // use rpush (append) so ref list sequence order is preserved
			
			// sanity check
			long listcnt = provider.llen(listkey);
			assertEquals (listcnt, SMALL_CNT, "list length should be SMALL_CNT");
			
			provider.ltrim(listkey, 0,listcnt-1);	// trim nothing
			assertEquals(provider.llen(listkey), listcnt, "trim from end to end - no delta expected");
			
			provider.ltrim(listkey, 1, listcnt-1); 	// remove the head
			assertEquals(provider.llen(listkey), listcnt-1, "trim head - len should be --1 expected");
			
			listcnt = provider.llen(listkey);
			assertEquals(listcnt, SMALL_CNT - 1, "list length should be SMALL_CNT - 1");
			for(int i=0; i<SMALL_CNT-1; i++)
				assertEquals(provider.lindex(listkey, i), dataList.get(i+1), "list items should match ref data shifted by 1 after removing head");
			
			provider.ltrim(listkey, -2, -1);
			assertEquals(provider.llen(listkey), 2, "list length should be 2");
			
			provider.ltrim(listkey, 0, 0);
			assertEquals(provider.llen(listkey), 1, "list length should be 1");

			byte[] lastItem = provider.lpop(listkey);
			assertNotNull(lastItem, "last item should not have been null");
			assertEquals(provider.llen(listkey), 0, "expecting empty list after trims and pop");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lindex(java.lang.String, long)}.
	 */
	@Test
	public void testLindex() {
		cmd = Command.LINDEX.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			// prep a small list
			String listkey = keys.get(0);
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, dataList.get(i)); // use rpush (append) so ref list sequence order is preserved
			
			for(int i=0; i<SMALL_CNT; i++)
				assertEquals(provider.lindex(listkey, i), dataList.get(i), "list items should match ref data");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lpop(java.lang.String)}.
	 */
	@Test
	public void testLpop() {
		cmd = Command.LPOP.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			// prep a small list
			String listkey = keys.get(0);
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, dataList.get(i)); // use rpush (append) so ref list sequence order is preserved
			
			// sanity check
			long listcnt = provider.llen(listkey);
			assertEquals (listcnt, SMALL_CNT, "list length should be SMALL_CNT");
			
			for(int i=0; i<SMALL_CNT; i++)
				assertEquals(provider.lpop(listkey), dataList.get(i), 
						"nth popped head should be the same as nth dataitem, where n is " + i);
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#rpop(java.lang.String)}.
	 */
	@Test
	public void testRpop() {
		cmd = Command.RPOP.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			// prep a small list
			String listkey = keys.get(0);
			for(int i=0; i<SMALL_CNT; i++)
				provider.lpush(listkey, dataList.get(i)); // use rpush (append) so ref list sequence order is preserved
			
			// sanity check
			long listcnt = provider.llen(listkey);
			assertEquals (listcnt, SMALL_CNT, "list length should be SMALL_CNT");
			
			for(int i=0; i<SMALL_CNT; i++)
				assertEquals(provider.rpop(listkey), dataList.get(i), 
						"nth popped tail should be the same as nth dataitem, where n is " + i);
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lrange(java.lang.String, int, int)}.
	 */
	@Test
	public void testLrange() {
		cmd = Command.LRANGE.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			// prep a MEDIUM list
			String listkey = keys.get(0);
			for(int i=0; i<MEDIUM_CNT; i++)
				provider.rpush(listkey, dataList.get(i)); // use rpush (append) so ref list sequence order is preserved
			
			// sanity check
			long listcnt = provider.llen(listkey);
			assertEquals (listcnt, MEDIUM_CNT, "list length should be MEDIUM_CNT");

			List<byte[]> items = provider.lrange(listkey, 0, SMALL_CNT-1);
			assertEquals (items.size(), SMALL_CNT, "list range 0->SMALL_CNT length should be SMALL_CNT");
			for(int i=0; i<SMALL_CNT; i++)
				assertEquals(items.get(i), dataList.get(i), 
						"nth items of range 0->CNT should be the same as nth dataitem, where n is " + i);
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lrem(java.lang.String, byte[], int)}.
	 */
	@Test
	public void testLremStringByteArrayInt() {
		cmd = Command.LREM.code + " byte[] | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<MEDIUM_CNT; i++)
				provider.rpush(listkey, dataList.get(i));
			assertTrue(provider.llen(listkey)==MEDIUM_CNT, "LLEN after RPUSH is wrong");
			
			// everysingle one of these should work and remove just 1 element
			assertEquals(1, provider.lrem(listkey, dataList.get(0), 0));
			assertEquals(1, provider.lrem(listkey, dataList.get(1), -1));
			assertEquals(1, provider.lrem(listkey, dataList.get(2), 1));
			assertEquals(1, provider.lrem(listkey, dataList.get(3), 2));
			assertEquals(1, provider.lrem(listkey, dataList.get(4), -2));
			
			// everysingle one of these should work and remove NOTHING
			assertEquals(0, provider.lrem(listkey, dataList.get(0), 0));
			assertEquals(0, provider.lrem(listkey, dataList.get(1), -1));
			assertEquals(0, provider.lrem(listkey, dataList.get(2), 1));
			assertEquals(0, provider.lrem(listkey, dataList.get(3), 2));
			assertEquals(0, provider.lrem(listkey, dataList.get(4), -2));
			
			// now we'll test to see how it handles empty lists
			provider.flushdb();
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, dataList.get(i));
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			for(int i=0; i<SMALL_CNT; i++)
				provider.lrem(listkey, dataList.get(i), 100);
			assertEquals(0, provider.llen(listkey), "LLEN should be zero");
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lrem(java.lang.String, java.lang.String, int)}.
	 */
	@Test
	public void testLremStringStringInt() {
		cmd = Command.LREM.code + " String | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<MEDIUM_CNT; i++)
				provider.rpush(listkey, stringList.get(i));
			assertTrue(provider.llen(listkey)==MEDIUM_CNT, "LLEN after RPUSH is wrong");
			
			// everysingle one of these should work and remove just 1 element
			assertEquals(1, provider.lrem(listkey, stringList.get(0), 0));
			assertEquals(1, provider.lrem(listkey, stringList.get(1), -1));
			assertEquals(1, provider.lrem(listkey, stringList.get(2), 1));
			assertEquals(1, provider.lrem(listkey, stringList.get(3), 2));
			assertEquals(1, provider.lrem(listkey, stringList.get(4), -2));
			
			// everysingle one of these should work and remove NOTHING
			assertEquals(0, provider.lrem(listkey, stringList.get(0), 0));
			assertEquals(0, provider.lrem(listkey, stringList.get(1), -1));
			assertEquals(0, provider.lrem(listkey, stringList.get(2), 1));
			assertEquals(0, provider.lrem(listkey, stringList.get(3), 2));
			assertEquals(0, provider.lrem(listkey, stringList.get(4), -2));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lrem(java.lang.String, java.lang.Number, int)}.
	 */
	@Test
	public void testLremStringNumberInt() {
		cmd = Command.LREM.code + " Number | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<MEDIUM_CNT; i++)
				provider.rpush(listkey, longList.get(i));
			assertTrue(provider.llen(listkey)==MEDIUM_CNT, "LLEN after RPUSH is wrong");
			
			// everysingle one of these should work and remove just 1 element
			assertEquals(1, provider.lrem(listkey, longList.get(0), 0));
			assertEquals(1, provider.lrem(listkey, longList.get(1), -1));
			assertEquals(1, provider.lrem(listkey, longList.get(2), 1));
			assertEquals(1, provider.lrem(listkey, longList.get(3), 2));
			assertEquals(1, provider.lrem(listkey, longList.get(4), -2));
			
			// everysingle one of these should work and remove NOTHING
			assertEquals(0, provider.lrem(listkey, longList.get(0), 0));
			assertEquals(0, provider.lrem(listkey, longList.get(1), -1));
			assertEquals(0, provider.lrem(listkey, longList.get(2), 1));
			assertEquals(0, provider.lrem(listkey, longList.get(3), 2));
			assertEquals(0, provider.lrem(listkey, longList.get(4), -2));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lrem(java.lang.String, java.io.Serializable, int)}.
	 */
	@Test
	public void testLremStringTInt() {
		cmd = Command.LREM.code + " Java Object | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<MEDIUM_CNT; i++)
				provider.rpush(listkey, objectList.get(i));
			assertTrue(provider.llen(listkey)==MEDIUM_CNT, "LLEN after RPUSH is wrong");
			
			// everysingle one of these should work and remove just 1 element
			assertEquals(1, provider.lrem(listkey, objectList.get(0), 0));
			assertEquals(1, provider.lrem(listkey, objectList.get(1), -1));
			assertEquals(1, provider.lrem(listkey, objectList.get(2), 1));
			assertEquals(1, provider.lrem(listkey, objectList.get(3), 2));
			assertEquals(1, provider.lrem(listkey, objectList.get(4), -2));
			
			// everysingle one of these should work and remove NOTHING
			assertEquals(0, provider.lrem(listkey, objectList.get(0), 0));
			assertEquals(0, provider.lrem(listkey, objectList.get(1), -1));
			assertEquals(0, provider.lrem(listkey, objectList.get(2), 1));
			assertEquals(0, provider.lrem(listkey, objectList.get(3), 2));
			assertEquals(0, provider.lrem(listkey, objectList.get(4), -2));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lset(java.lang.String, int, byte[])}.
	 */
	@Test
	public void testLsetStringIntByteArray() {
		cmd = Command.LSET.code + " byte[] | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, dataList.get(i));
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// now we'll change their values
			for(int i=0; i<SMALL_CNT; i++)
				provider.lset(listkey, i, dataList.get(SMALL_CNT+i));
			
			List<byte[]> range = null;
			
			range = provider.lrange(listkey, 0, LARGE_CNT);
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertEquals (dataList.get(SMALL_CNT+i), range.get(i), "after LSET the expected and range item differ at idx: " + i);
			
			// now we'll change their values using the negative index mode
			int lim = SMALL_CNT*-1;
			for(int i=-1; i>lim; i--)
				provider.lset(listkey, i, dataList.get(i*-1));

			range = provider.lrange(listkey, 0, LARGE_CNT);
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertEquals (dataList.get(SMALL_CNT-i), range.get(i), "after LSET the expected and range item differ at idx: " + i);
			
			// test edge conditions
			// out of range
			boolean expectedError = false;
			try {
				Log.log("Expecting an out of range ERROR for LSET here ..");
				provider.lset(listkey, SMALL_CNT, dataList.get(0)); 
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "out of range LSET index should have raised an exception but did not");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lset(java.lang.String, int, java.lang.String)}.
	 */
	@Test
	public void testLsetStringIntString() {
		cmd = Command.LSET.code + " String | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, stringList.get(i));
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// now we'll change their values
			for(int i=0; i<SMALL_CNT; i++)
				provider.lset(listkey, i, stringList.get(SMALL_CNT+i));
			
			List<String> range = null;
			
			range = toStr (provider.lrange(listkey, 0, LARGE_CNT));
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertTrue (stringList.get(SMALL_CNT+i).equals(range.get(i)), "after LSET the expected and range item differ at idx: " + i);
			
			// now we'll change their values using the negative index mode
			int lim = SMALL_CNT*-1;
			for(int i=-1; i>lim; i--)
				provider.lset(listkey, i, stringList.get(i*-1));

			range = toStr (provider.lrange(listkey, 0, LARGE_CNT));
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertTrue (stringList.get(SMALL_CNT-i).equals(range.get(i)), "after LSET the expected and range item differ at idx: " + i);
			
			// test edge conditions
			// out of range is same as byte[] as value type makes no difference
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lset(java.lang.String, int, java.lang.Number)}.
	 */
	@Test
	public void testLsetStringIntNumber() {
		cmd = Command.LSET.code + " Number | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, longList.get(i));
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// now we'll change their values
			for(int i=0; i<SMALL_CNT; i++)
				provider.lset(listkey, i, longList.get(SMALL_CNT+i));
			
			List<Long> range = null;
			
			range = toLong (provider.lrange(listkey, 0, LARGE_CNT));
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertTrue (longList.get(SMALL_CNT+i).equals(range.get(i)), "after LSET the expected and range item differ at idx: " + i);
			
			// now we'll change their values using the negative index mode
			int lim = SMALL_CNT*-1;
			for(int i=-1; i>lim; i--)
				provider.lset(listkey, i, longList.get(i*-1));

			range = toLong (provider.lrange(listkey, 0, LARGE_CNT));
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertTrue (longList.get(SMALL_CNT-i).equals(range.get(i)), "after LSET the expected and range item differ at idx: " + i);
			
			// test edge conditions
			// out of range is same as byte[] as value type makes no difference
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#lset(java.lang.String, int, java.io.Serializable)}.
	 */
	@Test
	public void testLsetStringIntT() {
		cmd = Command.LSET.code + " Java Object | " + Command.LLEN;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			String listkey = this.keys.get(0);
			// we'll make a list of unique items first
			for(int i=0; i<SMALL_CNT; i++)
				provider.rpush(listkey, objectList.get(i));
			assertTrue(provider.llen(listkey)==SMALL_CNT, "LLEN after RPUSH is wrong");
			
			// now we'll change their values
			for(int i=0; i<SMALL_CNT; i++)
				provider.lset(listkey, i, objectList.get(SMALL_CNT+i));
			
			List<TestBean> range = null;
			
			range = decode (provider.lrange(listkey, 0, LARGE_CNT));
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertTrue (objectList.get(SMALL_CNT+i).equals(range.get(i)), "after LSET the expected and range item differ at idx: " + i);
			
			// now we'll change their values using the negative index mode
			int lim = SMALL_CNT*-1;
			for(int i=-1; i>lim; i--)
				provider.lset(listkey, i, objectList.get(i*-1));

			range = decode (provider.lrange(listkey, 0, LARGE_CNT));
			assertEquals (SMALL_CNT, range.size(), "range length is wrong");
			for(int i=0; i<SMALL_CNT; i++)
				assertTrue (objectList.get(SMALL_CNT-i).equals(range.get(i)), "after LSET the expected and range item differ at idx: " + i);
			
			// test edge conditions
			// out of range is same as byte[] as value type makes no difference
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}


	/**************** QUERY COMMANDS ******************************/
	/**
	 * This command is still half-baked on the Redis side, so we just test to see if
	 * it blows up or not.  (cooking:  if you sort on a set/list of size N and your
	 * constrains (GET) limit the actual results to nothing, Redis (0.091) returns a 
	 * list of size N full of nulls.  That's not tasty ..)
	 * 
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sort(java.lang.String)}.
	 */
	@Test
	public void testSort() {
		cmd = Command.SORT.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = "set-key";
			String listkey = "list-key";
			for(int i=0; i<MEDIUM_CNT; i++){
				provider.sadd(setkey, stringList.get(i));
				provider.lpush(listkey, stringList.get(i));
			}

			List<String> sorted = null;
			
			Log.log("TEST: SORTED LIST ");
//			sorted = toStr(jredis.sort(listkey).ALPHA().LIMIT(0, 100).BY("*A*").exec());
			sorted = toStr(provider.sort(listkey).ALPHA().LIMIT(0, 555).DESC().exec());
			for(String s : sorted)
				System.out.format("%s\n", s);
			
			Log.log("TEST: SORTED SET ");
//			sorted = toStr(jredis.sort(setkey).ALPHA().LIMIT(0, 100).BY("*BB*").exec());
			sorted = toStr(provider.sort(setkey).ALPHA().LIMIT(0, 555).DESC().exec());
			for(String s : sorted)
				System.out.format("%s\n", s);
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	
	/**************** SET COMMANDS ******************************/
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sadd(java.lang.String, byte[])}.
	 */
	@Test
	public void testSaddStringByteArray() {
		cmd = Command.SADD.code + " byte[]";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, dataList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertFalse(provider.sadd(setkey, dataList.get(i)), "sadd of existing element should be false");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sadd(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testSaddStringString() {
		cmd = Command.SADD.code + " String";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, stringList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertFalse(provider.sadd(setkey, stringList.get(i)), "sadd of existing element should be false");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sadd(java.lang.String, java.lang.Number)}.
	 */
	@Test
	public void testSaddStringNumber() {
		cmd = Command.SADD.code + " Number";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, longList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertFalse(provider.sadd(setkey, longList.get(i)), "sadd of existing element should be false");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sadd(java.lang.String, java.io.Serializable)}.
	 */
	@Test
	public void testSaddStringT() {
		cmd = Command.SADD.code + " Java Object";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, objectList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertFalse(provider.sadd(setkey, objectList.get(i)), "sadd of existing element should be false");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#smembers(java.lang.String)}.
	 */
	@Test
	public void testSmembers() {
		cmd = Command.SMEMBERS.code + " byte[] ";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, dataList.get(i)), "sadd of random element should be true");
			
			List<byte[]> members = null;
			members = provider.smembers(setkey);
			assertTrue(members.size() == SMALL_CNT);
			// byte[] don't play nice with equals -- values are random so if size matches, its ok
//			for(int i=0;i<SMALL_CNT; i++)
//				assertTrue(members.contains(dataList.get(i)), "set members should include item at idx: " + i);

			// test edget conditions
			// empty set
			provider.sadd(keys.get(2), dataList.get(0));
			provider.srem(keys.get(2), dataList.get(0));
			assertTrue(provider.scard(keys.get(2)) == 0, "set should be empty now");
			members = provider.smembers(keys.get(2));
			assertNotNull(members, "smembers should return an empty set, not null");
			assertTrue(members.size() == 0, "smembers should have returned an empty list");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }

		cmd = Command.SMEMBERS.code + " String ";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, stringList.get(i)), "sadd of random element should be true");
			
			List<String> members = null;
			members = toStr(provider.smembers(setkey));
			assertTrue(members.size() == SMALL_CNT);

			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(members.contains(stringList.get(i)), "set members should include item at idx: " + i);

			// test edget conditions
			// empty set
			provider.sadd(keys.get(2), stringList.get(0));
			provider.srem(keys.get(2), stringList.get(0));
			assertTrue(provider.scard(keys.get(2)) == 0, "set should be empty now");
			members = toStr(provider.smembers(keys.get(2)));
			assertNotNull(members, "smembers should return an empty set, not null");
			assertTrue(members.size() == 0, "smembers should have returned an empty list");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }

		cmd = Command.SMEMBERS.code + " Number ";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, longList.get(i)), "sadd of random element should be true");
			
			List<Long> members = null;
			members = toLong (provider.smembers(setkey));
			assertTrue(members.size() == SMALL_CNT);

			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(members.contains(longList.get(i)), "set members should include item at idx: " + i);

			// test edget conditions
			// empty set
			provider.sadd(keys.get(2), longList.get(0));
			provider.srem(keys.get(2), longList.get(0));
			assertTrue(provider.scard(keys.get(2)) == 0, "set should be empty now");
			members = toLong (provider.smembers(keys.get(2)));
			assertNotNull(members, "smembers should return an empty set, not null");
			assertTrue(members.size() == 0, "smembers should have returned an empty list");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }

		cmd = Command.SMEMBERS.code + " Java Object ";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, objectList.get(i)), "sadd of random element should be true");
			
			List<TestBean> members = null;
			members = decode (provider.smembers(setkey));
			assertTrue(members.size() == SMALL_CNT);

			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(members.contains(objectList.get(i)), "set members should include item at idx: " + i);

			// test edget conditions
			// empty set
			provider.sadd(keys.get(2), objectList.get(0));
			provider.srem(keys.get(2), objectList.get(0));
			assertTrue(provider.scard(keys.get(2)) == 0, "set should be empty now");
			members = decode (provider.smembers(keys.get(2)));
			assertNotNull(members, "smembers should return an empty set, not null");
			assertTrue(members.size() == 0, "smembers should have returned an empty list");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sismember(java.lang.String, byte[])}.
	 */
	@Test
	public void testSmoveStringByteArray() {
		cmd = Command.SMOVE.code + " byte[]";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String srckey = keys.get(0);
			String destkey = keys.get(1);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(srckey, dataList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++) {
				assertTrue(provider.sismember(srckey, dataList.get(i)), "should be a member of the src before move");
				
				/* smove */
				assertTrue(provider.smove (srckey, destkey, dataList.get(i)), "move should be ok");
				
				assertTrue(provider.sismember(destkey, dataList.get(i)), "should be a member of the dest after move");
				assertFalse(provider.sismember(srckey, dataList.get(i)), "should NOT be a member of the src after move");
			}
			
			// lets try the error conditions by using wrong type for src or dest
			boolean expectedError;
			
			String stringKey = "foo";
			provider.set(stringKey, "smove test");
			
			String listKey = "bar";
			provider.lpush(listKey, "smove test");
			
			// wrong dest
			expectedError = false;
			try {
				Log.log("Expecting an operation against key holding the wrong kind of value ERROR..");
				assertTrue(provider.smove (destkey, stringKey, dataList.get(0)), "dest is wrong type");
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "should have raised an exception but did not");
			
			// wrong src
			expectedError = false;
			try {
				Log.log("Expecting an operation against key holding the wrong kind of value ERROR..");
				assertTrue(provider.smove (stringKey, srckey, dataList.get(0)), "src is wrong type");
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "should have raised an exception but did not");
			
			// wrong src and dest
			expectedError = false;
			try {
				Log.log("Expecting an operation against key holding the wrong kind of value ERROR..");
				assertTrue(provider.smove (listKey, stringKey, dataList.get(0)), "src and dest are wrong type");
			}
			catch (RedisException e) { expectedError = true; }
			assertTrue(expectedError, "should have raised an exception but did not");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sismember(java.lang.String, byte[])}.
	 */
	@Test
	public void testSismemberStringByteArray() {
		cmd = Command.SISMEMBER.code + " byte[]";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, dataList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sismember(setkey, dataList.get(i)), "should be a member of the set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sismember(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testSismemberStringString() {
		cmd = Command.SISMEMBER.code + " String";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, stringList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sismember(setkey, stringList.get(i)), "should be a member of the set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sismember(java.lang.String, java.lang.Number)}.
	 */
	@Test
	public void testSismemberStringNumber() {
		cmd = Command.SISMEMBER.code + " Number";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, longList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sismember(setkey, longList.get(i)), "should be a member of the set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sismember(java.lang.String, java.io.Serializable)}.
	 */
	@Test
	public void testSismemberStringT() {
		cmd = Command.SISMEMBER.code + " Java Object";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, objectList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sismember(setkey, objectList.get(i)), "should be a member of the set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}


	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#scard(java.lang.String)}.
	 */
	@Test
	public void testScard() {
		cmd = Command.SCARD.code + " Java Object";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++)
				assertTrue(provider.sadd(setkey, dataList.get(i)), "sadd of random element should be true");
			
			assertEquals (provider.scard (setkey), SMALL_CNT, "scard should be SMALL_CNT");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sinter(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSinter() {
		cmd = Command.SINTER.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey1 = keys.get(0);
			String setkey2 = keys.get(1);
			String setunique = keys.get(2);
			for(int i=0;i<SMALL_CNT; i++) {
				assertTrue(provider.sadd(setkey1, dataList.get(i)), "sadd of random element should be true");
				assertTrue(provider.sadd(setkey2, dataList.get(i+2)), "sadd of random element should be true");
				assertTrue(provider.sadd(setunique, dataList.get(10+i+SMALL_CNT)), "sadd of random element should be true");
			}
			assertEquals (0, provider.sinter(setkey1, setkey2, setunique).size(), "should be no common elements in all three");
			assertTrue (provider.sinter(setkey1, setkey2).size() > 0, "should be common elements in set 1 and 2");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sinterstore(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSinterstore() {
		cmd = Command.SINTERSTORE.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey1 = keys.get(0);
			String setkey2 = keys.get(1);
			String setunique = keys.get(2);
			String interset = keys.get(3);
			for(int i=0;i<SMALL_CNT; i++) {
				assertTrue(provider.sadd(setkey1, dataList.get(i)), "sadd of random element should be true");
				assertTrue(provider.sadd(setkey2, dataList.get(i+2)), "sadd of random element should be true");
				assertTrue(provider.sadd(setunique, dataList.get(10+i+SMALL_CNT)), "sadd of random element should be true");
			}
			provider.sinterstore (interset, setkey1, setkey2, setunique);
			assertEquals (0, provider.scard(interset), "interset set should be empty");
			provider.sinterstore (interset, setkey1, setkey2);
			assertTrue (provider.scard(interset) > 0, "interset set should be non-empty");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}


	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sunion(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSunion() {
		cmd = Command.SUNION.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey1 = keys.get(0);
			String setkey2 = keys.get(1);
			String setunique = keys.get(2);
			for(int i=0;i<SMALL_CNT; i++) {
				assertTrue(provider.sadd(setkey1, dataList.get(i)), "sadd of random element should be true");
				assertTrue(provider.sadd(setkey2, dataList.get(i)), "sadd of random element should be true");
				assertTrue(provider.sadd(setunique, stringList.get(i)), "sadd of random element should be true");
			}
			assertEquals (SMALL_CNT, provider.sunion (setkey1, setkey2).size(), "union of equiv sets should have same card as the two");
			assertEquals (SMALL_CNT*2, provider.sunion (setkey1, setkey2, setunique).size(), "union of all 3 sets should have SMALL_CNT * 2 members");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sunionstore(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSunionstore() {
		cmd = Command.SUNIONSTORE.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey1 = keys.get(0);
			String setkey2 = keys.get(1);
			String setunique = keys.get(2);
			String union = keys.get(3);
			for(int i=0;i<SMALL_CNT; i++) {
				assertTrue(provider.sadd(setkey1, dataList.get(i)), "sadd of random element should be true");
				assertTrue(provider.sadd(setkey2, dataList.get(i)), "sadd of random element should be true");
				assertTrue(provider.sadd(setunique, stringList.get(i)), "sadd of random element should be true");
			}
			provider.sunionstore (union, setkey1, setkey2);
			assertEquals (SMALL_CNT, provider.scard(union), "union of equiv sets should have same card as the two");
			provider.sunionstore (union, setkey1, setkey2, setunique);
			assertEquals (SMALL_CNT*2, provider.scard(union), "union of all 3 sets should have SMALL_CNT * 2 members");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}


	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sdiff(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSdiff() {
		cmd = Command.SDIFF.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey1 = keys.get(0);
			String setkey2 = keys.get(1);
			String setkey3 = keys.get(2);
			String setexpectedkey = keys.get(3);
//			
			// - per the redis doc -- 
			// note that basically, SDIFF k, k1, ..., kn is a diff between k and union (k1, .., kn)
			//
			provider.sadd(setkey1, "x");
			provider.sadd(setkey1, "a");
			provider.sadd(setkey1, "b");
			provider.sadd(setkey1, "c");
			
			provider.sadd(setkey2, "c");

			provider.sadd(setkey3, "a");
			provider.sadd(setkey3, "d");
			
			provider.sadd(setexpectedkey, "x");
			provider.sadd(setexpectedkey, "b");
			
			List<String> sdiffResults = DefaultCodec.toStr(provider.sdiff(setkey1, setkey2, setkey3));
			assertEquals(provider.scard(setexpectedkey), sdiffResults.size(), "sdiff result and expected set should have same cardinality");
			for(String s : sdiffResults)
				assertTrue(provider.sismember(setexpectedkey, s), s + " should be a member of the expected result set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#sdiff(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSdiffstore() {
		cmd = Command.SDIFFSTORE.code;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey1 = keys.get(0);
			String setkey2 = keys.get(1);
			String setkey3 = keys.get(2);
			String setdiffreskey = keys.get(3);
//			
			// - per the redis doc -- 
			// note that basically, SDIFF k, k1, ..., kn is a diff between k and union (k1, .., kn)
			//
			provider.sadd(setkey1, "x");
			provider.sadd(setkey1, "a");
			provider.sadd(setkey1, "b");
			provider.sadd(setkey1, "c");
			
			provider.sadd(setkey2, "c");

			provider.sadd(setkey3, "a");
			provider.sadd(setkey3, "d");
						
			provider.sdiffstore (setdiffreskey, setkey1, setkey2, setkey3);
			assertEquals(provider.scard(setdiffreskey), provider.sdiff(setkey1, setkey2, setkey3).size(), "sdiff result and sdiffstore dest set should have same cardinality");
			assertTrue(provider.sismember(setdiffreskey, "x"), "x should be a member of the expected result set");
			assertTrue(provider.sismember(setdiffreskey, "b"), "b should be a member of the expected result set");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#srem(java.lang.String, byte[])}.
	 */
	@Test
	public void testSremStringByteArray() {
		cmd = Command.SISMEMBER.code + " byte[]";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.sadd(setkey, dataList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.srem(setkey, dataList.get(i)), "should be a removable member of the set");
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#srem(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testSremStringString() {
		cmd = Command.SISMEMBER.code + " String";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.sadd(setkey, stringList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.srem(setkey, stringList.get(i)), "should be a removable member of the set");
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#srem(java.lang.String, java.lang.Number)}.
	 */
	@Test
	public void testSremStringNumber() {
		cmd = Command.SISMEMBER.code + " Number";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.sadd(setkey, longList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.srem(setkey, longList.get(i)), "should be a removable member of the set");
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#srem(java.lang.String, java.io.Serializable)}.
	 */
	@Test
	public void testSremStringT() {
		cmd = Command.SISMEMBER.code + " Java Object";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			String setkey = keys.get(0);
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.sadd(setkey, objectList.get(i)), "sadd of random element should be true");
			
			for(int i=0;i<SMALL_CNT; i++) 
				assertTrue(provider.srem(setkey, objectList.get(i)), "should be a removable member of the set");
			
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
	
	/************************ DB COMMANDS ***********************/
	
	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#dbsize()}.
	 */
	@Test
	public void testDbsize() {
		cmd = Command.DBSIZE.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.flushdb();
			assertTrue (provider.dbsize() == 0);
			
			for (int i=0; i<SMALL_CNT; i++)
				provider.set(keys.get(i), dataList.get(i));
			
			assertTrue (provider.dbsize() == SMALL_CNT, "dbsize should be SMALL_CNT");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#randomkey()}.
	 */
	@Test
	public void testRandomkey() {
		cmd = Command.RANDOMKEY.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			assertTrue (provider.dbsize() == 0);
			
			String iamempty = provider.randomkey();
			assertEquals(0, iamempty.length(), "randomkey of an empty db should be a zero length result");
			
			for (int i=0; i<MEDIUM_CNT; i++)
				provider.set(keys.get(i), dataList.get(i));
			
			assertTrue (provider.dbsize() == MEDIUM_CNT, "dbsize should be MEDIUM_CNT");
			for (int i=0; i<SMALL_CNT; i++) {
				assertTrue(keys.contains(provider.randomkey()), "randomkey should be an item in our keys list");
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
//	/**
//	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#move(java.lang.String, int)}.
//	 */
//	@Test
//	public void testMove() {
//		test = Command.MOVE.code ;
//		Log.log("TEST: %s command", test);
//		try {
//			jredis.flushdb();
//			assertTrue (jredis.dbsize() == 0, "db1 should be empty");
//			
//			jredis.select(db2).flushdb();
//			assertTrue (jredis.dbsize() == 0, "db2 should be empty");
//			
//			jredis.set(keys.get(0), dataList.get(0));
//			assertTrue (jredis.dbsize() == 1, "db2 should have 1 key at this point");
//			
//			jredis.move(keys.get(0), db1);
//			assertTrue (jredis.dbsize() == 0, "db2 should be empty again");
//			jredis.select(db1);
//			assertTrue (jredis.dbsize() == 1, "db1 should have 1 key at this point");
//			
//		} 
//		catch (RedisException e) { fail(test + " ERROR => " + e.getLocalizedMessage(), e); }
//	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#type(java.lang.String)}.
	 */
	@Test
	public void testType() {
		cmd = Command.TYPE.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			provider.set(keys.get(0), dataList.get(0));
			provider.sadd(keys.get(1), dataList.get(1));
			provider.rpush(keys.get(2), dataList.get(2));
			
			assertTrue(provider.type(keys.get(0))==RedisType.string, "type should be string");
			assertTrue(provider.type(keys.get(1))==RedisType.set, "type should be set");
			assertTrue(provider.type(keys.get(2))==RedisType.list, "type should be list");
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#info()}.
	 */
	@Test
	public void testInfo() {
		cmd = Command.INFO.code ;
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();

			Map<String, String> infoMap =  provider.info();
			for (RedisInfo info : RedisInfo.values()){
				assertNotNull(infoMap.get(info.name()));
				Log.log("%s => %s", info.name(), infoMap.get(info.name()));
			}
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#keys()}.
	 */
	@Test
	public void testKeys() {
		cmd = Command.KEYS.code + " (*)";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			for (int i=0; i<SMALL_CNT; i++)
				provider.set(keys.get(i), dataList.get(i));

			List<String> rediskeys = provider.keys();
			assertEquals(SMALL_CNT, rediskeys.size(), "size of key list should be SMALL_CNT");
			for(int i=0; i<SMALL_CNT; i++) 
				assertTrue(rediskeys.contains(keys.get(i)), "should contain " + keys.get(i));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}

	/**
	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#keys(java.lang.String)}.
	 */
	@Test
	public void testKeysString() {
		cmd = Command.KEYS.code + " (using patterns)";
		Log.log("TEST: %s command", cmd);
		try {
			provider.flushdb();
			
			for (int i=0; i<SMALL_CNT; i++)
				provider.set(patternList.get(i), dataList.get(i));

			List<String> rediskeys = provider.keys("*"+patternA+"*");
			assertEquals(SMALL_CNT, rediskeys.size(), "size of key list should be SMALL_CNT");
			for(int i=0; i<SMALL_CNT; i++) 
				assertTrue(rediskeys.contains(patternList.get(i)), "should contain " + patternList.get(i));
		} 
		catch (RedisException e) { fail(cmd + " ERROR => " + e.getLocalizedMessage(), e); }
	}
//	/**
//	 * Test method for {@link org.jredis.ri.alphazero.JRedisSupport#shutdown()}.
//	 */
//	@Test
//	public void testShutdown() {
//		fail("Not yet implemented");
//	}
//



	// ========================================================================
	// Test Properties
	// ========================================================================
	
	/** the JRedis implementation being tested */
//	private JRedis provider = null;
	
	// ------------------------------------------------------------------------
	// JRedis Provider initialize methods
	// ------------------------------------------------------------------------

//	/**
//	 * Sets the {@link JRedis} implementation provider for the test suite
//	 */
//	@BeforeTest
//	public void setJRedisProvider () {
//		try {
//			JRedis jredis = newJRedisProviderInstance();
//
//			setJRedisProviderInstance(jredis);
//			prepTestDBs();
//			
//			Log.log("JRedisClientNGTest.setJRedisProvider - done");
//        }
//        catch (ClientRuntimeException e) {
//        	Log.error(e.getLocalizedMessage());
//        }
//	}
//	
//	/**
//	 * Extension point:  Tests for specific implementations of {@link JRedis} 
//	 * implement this method to create the provider instance.
//	 * @return {@link JRedis} implementation instance
//	 */
//	protected abstract JRedis newJRedisProviderInstance () ;
//	
//	/**
//	 * Must be called by a BeforeTest method to set the jredis parameter.
//	 * @param jredisProvider that is being tested.
//	 */
//	protected final void setJRedisProviderInstance (JRedis jredisProvider) {
//		this.jredis = jredisProvider;
//		Log.log( "TEST: " +
//				"\n\t-----------------------------------------------\n" +
//				"\tProvider Class: %s" +
//				"\n\t-----------------------------------------------\n", 
//				jredisProvider.getClass().getCanonicalName());
//	}
//	/**
//	 * @return the {@link JRedis} instance used for the provider tests
//	 */
//	protected final JRedis getJRedisProviderInstance() {
//		return jredis;
//	}
}