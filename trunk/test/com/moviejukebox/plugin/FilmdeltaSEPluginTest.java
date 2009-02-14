package com.moviejukebox.plugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;

public class FilmdeltaSEPluginTest extends TestCase {

	private static final Logger logger = Logger.getLogger("moviejukebox"); 
	private Movie movie;

	FilmDeltaSEPluginMock filmdeltaPlugin = null;
	
	protected void setUp() throws Exception {
		logger.setLevel(Level.FINEST);
		
		//Set properties..
		String propertiesName = "moviejukebox.properties";
		if (!PropertiesUtil.setPropertiesStreamName(propertiesName)) {
			logger.severe("Failed instantiating MovieDatabasePlugin: ");
        }
		
        //Create an instance of the mock for the FilmDeltaSEPlugin
		//offline = true uses mocked data
		//offline = false gets the data directly from internet
        filmdeltaPlugin = new FilmDeltaSEPluginMock();
        filmdeltaPlugin.setOffline(true);
		
		//create a movie object
		movie = new Movie();
	}
	public void testCreateFilmDeltaSEPlugin() {
		//instantiera plugin movieDBPlugin
		try {
            MovieDatabasePlugin filmDeltaSEPlugin = new FilmDeltaSEPlugin();
            assertFalse(filmDeltaSEPlugin == null);
        } catch (Exception e) {
        	fail("Error instantiating FilmDeltaSEPlugin");
        }
	}
	public void testGetFilmdeltaId() {
		filmdeltaPlugin.setRequestResult("<h2 class=hd>Search Results</h2><div><ol><li class=g><h3 class=r><a href=\"http://www.filmdelta.se/filmer/146410/lat_den_ratte_komma_in/\" class=l onmousedown=\"return clk(this.href,\'\',\'\',\'res\',\'1\',\'\')\"><em>");
        assertEquals("146410/lat_den_ratte_komma_in", filmdeltaPlugin.getFilmdeltaId("låt den rätte", "", 0));
	}
	
	public void testGetFilmdeltaIdWithTitleAndYear() {
		filmdeltaPlugin.setRequestResult("<div id=res class=med><h2 class=hd>Search Results</h2><div><ol><li class=g><h3 class=r><a href=\"http://www.filmdelta.se/filmer/145614/wall-e/\" class=l ");
        assertEquals("145614/wall-e", filmdeltaPlugin.getFilmdeltaId("wall-e", "2008", 0));
	}
	
	
	public void testGetFilmdeltaIdNoMatch() {
		filmdeltaPlugin.setRequestResult("<b>apo panda site:filmdelta.se/filmer</b> - did not match any documents.  <p style=margin-top:1em>Suggestions:<ul><li>Make sure all words are spelled correctly.<li>Try");
		assertEquals("UNKNOWN", filmdeltaPlugin.getFilmdeltaId("apo panda", "", 0));
	}
	public void testUpdateFilmdeltaMediaInfo() {
		//set up
		movie.setTitle("Den lilla sjöjungfrun");
		File resultFile = new File("test/FilmDeltaSEPluginTestFixtures/den_lilla_sjojungfrun.html");
		filmdeltaPlugin.setRequestResult(FileTools.readFileToString(resultFile));
		//run update function
		filmdeltaPlugin.updateFilmdeltaMediaInfo(movie, "15353/den_lilla_sjojungfrun");
		//verify results
		assertEquals("Den lilla sjöjungfrun", movie.getTitle());
		assertEquals("1989", movie.getYear());
		assertEquals("82", movie.getRuntime());
		assertEquals(76, movie.getRating());
		assertEquals("USA", movie.getCountry());
		assertEquals("Sjöjungfrun Ariel drömmer", movie.getPlot().substring(0, 25));
		assertEquals("John Musker / Ron Clements", movie.getDirector());
		//assertEquals("René Auberjonois, Christopher Daniel Barnes, ", movie.getCast().toString().substring(0, 46));
	}
	
	public void testUpdateFilmdeltaMediaInfoMovieWithIncompleteMovie() {
		//set up
		movie.setTitle("barbie som tiggarflickan");
		File resultFile = new File("test/FilmDeltaSEPluginTestFixtures/barbie_tiggarflickan.html");
		filmdeltaPlugin.setRequestResult(FileTools.readFileToString(resultFile));
		//run update function
		filmdeltaPlugin.updateFilmdeltaMediaInfo(movie, "127907/barbie_som_prinsessan_och_tiggarflickan");
		//verify results
		assertEquals("Barbie som prinsessan och tiggarflickan", movie.getTitle());
		assertEquals("UNKNOWN", movie.getYear());
		assertEquals(-1, movie.getRating());
		assertEquals("UNKNOWN", movie.getRuntime());
		assertEquals("UNKNOWN", movie.getCountry());
		assertEquals("Flickornas öden korsas när prinsessan An", movie.getPlot().substring(0, 40));
		assertEquals("UNKNOWN", movie.getDirector());
	}
	
	
	public void testScanTvShow() {
		movie.setTitle("dexter");
		movie.setSeason(1);
		movie.setPlot("This is a plot from tvdb");
		//mock search result from google
		//no mocking of thetvdb as of now
		filmdeltaPlugin.setRequestResult("<h2 class=hd>Search Results</h2><div><ol><li class=g><h3 class=r><a href=\"http://www.filmdelta.se/filmer/146818/dexter-sasong_1/\" class=l onmousedown=\"return clk(this.href");
		boolean isScanned = filmdeltaPlugin.scan(movie);
		assertTrue(isScanned);
		assertEquals("Dexter", movie.getTitle());
		assertEquals("2006", movie.getYear());
		assertEquals("146818/dexter-sasong_1", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
	}

	public void testScanNFO() {
        String nfo_mermaid = 
        	FileTools.readFileToString(new File("test/FilmDeltaSEPluginTestFixtures/den_lilla_sjojungfrun.nfo"));
		filmdeltaPlugin.scanNFO(nfo_mermaid, movie);
		assertEquals("15353/den_lilla_sjojungfrun", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
	}
	
	public void testScanNFOAndGetFilmdeltaId() {
		//this only works in online mode
		if(!filmdeltaPlugin.isOffline()) {
			//set title to one that is not found by getFilmdeltaId-function
			movie.setTitle("bulgur walle");

			//get info from nfo (contains only link to imdb) 
			String nfo_dalma = 
				FileTools.readFileToString(new File("test/FilmDeltaSEPluginTestFixtures/bulgur-walle.nfo"));
			filmdeltaPlugin.scanNFO(nfo_dalma, movie);
			
			//check that movie was found via info in nfo
			boolean isScanned = false; 
			isScanned = filmdeltaPlugin.scan(movie);
			assertTrue(isScanned);
			assertEquals("145614/wall-e", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
			assertEquals("tt0910970", movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
			assertEquals("Wall-E", movie.getTitle());			
		} else {
			assertNotNull("Always pass in offline mode");
		}

	}
	
	public void testGetCdonPosterSuccess() {
		fail("Not yet implemented");
	}
	public void testGetCdonPosterNotFound() {
		fail("Not yet implented");
	}
	
}
