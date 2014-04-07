package uk.ac.cam.cl.dtg.segue.database;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.easymock.EasyMock;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Test;
import org.junit.runner.RunWith;


import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.legacy.PowerMockRunner;
 
@RunWith(PowerMockRunner.class)
@PrepareForTest(Git.class)
public class GitDbTest {

	@Test
	public void testGitDbString() throws IOException {
		PowerMock.mockStatic(Git.class);
		
		// Test that if you provide an empty string or null, an IllegalArgumentException gets thrown and git.open never gets called. 
		
		PowerMock.replay(Git.class);
		
		try {
			GitDb gitDb = new GitDb("");
			fail("GitDb constructor was given an empty string, but didn't throw an exception");
		} catch (IllegalArgumentException e) { 
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}
		
		try {
			GitDb gitDb = new GitDb((String)null);
			fail("GitDb constructor was given null, but didn't throw an exception");
		} catch (NullPointerException e) { 
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}
	}

	@Test
	public void testGitDbStringStringString() {
		// Test that if you provide an empty string or null, an IllegalArgumentException gets thrown and git.open never gets called. 
		
		PowerMock.replay(Git.class);
		
		try {
			GitDb gitDb = new GitDb("", null, null);
			fail("GitDb constructor was given an empty string, but didn't throw an exception");
		} catch (IllegalArgumentException e) { 
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}
		
		try {
			GitDb gitDb = new GitDb(null, null, null);
			fail("GitDb constructor was given null, but didn't throw an exception");
		} catch (NullPointerException e) { 
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}
		
	}

	@Test
	public void testGetFileByCommitSHA() {
		// TODO Test getFileByCommitSHA method
	}

	@Test
	public void testGetTreeWalk() throws IOException {
		
		Git git = EasyMock.createMock(Git.class);
		
		GitDb db = new GitDb(git);
		TreeWalk tw;
		
		try {
			tw = db.getTreeWalk("", "");
			fail("Failed to throw required exception on blank sha.");
		} catch (IllegalArgumentException e) {
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("Wrong type of exception thrown on blank sha");
		}		
		
		try {
			tw = db.getTreeWalk(null, "");
			fail("Failed to throw required exception on null sha.");
		} catch (NullPointerException e) {
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("Wrong type of exception thrown on null sha");
		}
		
		try {
			tw = db.getTreeWalk("sha", null);
			fail("Failed to throw required exception on null path.");
		} catch (NullPointerException e) {
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("Wrong type of exception thrown on null path");
		}		

		Repository repo = EasyMock.createMock(Repository.class);
		
		EasyMock.expect(git.getRepository()).andReturn(repo);
		EasyMock.expect(repo.resolve("sha")).andReturn(null);
		
		EasyMock.replay(git);
		EasyMock.replay(repo);
		
		assertNull(db.getTreeWalk("sha", "")); // Blank path is explicitly allowed. This should not throw an exception. But in this case we've passed an invalid sha, so we should get null back.
		
	}

	@Test
	public void testGetGitRepository() {
		// TODO Test getGitRepository method
	}

	@Test
	public void testVerifyGitObject() {
		// TODO Test verifyGitObject method
	}

	@Test
	public void testVerifyCommitExists() {
		// TODO Test verifyCommitExists method
	}

	@Test
	public void testListCommits() {
		// TODO Test listCommits method
	}

	@Test
	public void testPullLatestFromRemote() {
		// TODO Test pullLatestFromRemote method
	}

	@Test
	public void testGetHeadSha() {
		// TODO Test getHeadSha method
	}

}
