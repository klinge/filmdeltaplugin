package com.moviejukebox.plugin;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for com.moviejukebox.plugin");
		//$JUnit-BEGIN$
		suite.addTestSuite(FilmwebPluginTest.class);
		suite.addTestSuite(FilmdeltaSEPluginTest.class);
		//$JUnit-END$
		return suite;
	}

}
