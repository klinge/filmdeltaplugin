package com.moviejukebox.plugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Unit test for FilmDeltaSePlugin class
 * Depends on FilmDeltaSePluginMock
 * 
 * To run tests against online webpages (filmdelta and cdon) set 
 * "filmdeltaPlugin.setOffline(false)"
 * 
 * @author  johan.klinge
 * @version 0.1, 16th February 2009
 */
public class FilmdeltaSEPluginTest extends TestCase {

	private static final Logger logger = Logger.getLogger("moviejukebox"); 
	private Movie movie;

	FilmDeltaSEPluginMock filmdeltaPlugin = null;
	
	protected void setUp() throws Exception {
		logger.setLevel(Level.FINEST);
		//Set properties..
		String propertiesName = "moviejukebox.properties";
		if (!PropertiesUtil.setPropertiesStreamName(propertiesName)) {
			logger.severe("Failed setting properties");
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
		//instantiate movieDBPlugin
		try {
            MovieDatabasePlugin filmDeltaSEPlugin = new FilmDeltaSEPlugin();
            assertEquals("class com.moviejukebox.plugin.FilmDeltaSEPlugin", filmDeltaSEPlugin.getClass().toString());
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
		//this test makes an online call to the tvdb - no mocking of that as of now
		movie.setTitle("dexter");
		movie.setSeason(1);
		movie.setPlot("This is a plot from tvdb");
		//mock search result from google
		filmdeltaPlugin.setRequestResult("<h2 class=hd>Search Results</h2><div><ol><li class=g><h3 class=r><a href=\"http://www.filmdelta.se/filmer/146818/dexter-sasong_1/\" class=l onmousedown=\"return clk(this.href");
		boolean isScanned = filmdeltaPlugin.scan(movie);
		assertTrue(isScanned);
		assertEquals("Dexter", movie.getTitle());
		assertEquals("2006", movie.getYear());
		//assertEquals("146818/dexter-sasong_1", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
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
			String nfo_walle = 
				FileTools.readFileToString(new File("test/FilmDeltaSEPluginTestFixtures/bulgur-walle.nfo"));
			filmdeltaPlugin.scanNFO(nfo_walle, movie);
			
			//check that movie was found via info in nfo
			boolean isScanned = false; 
			isScanned = filmdeltaPlugin.scan(movie);
			assertTrue(isScanned);
			assertEquals("145614/wall-e", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
			assertEquals("tt0910970", movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
			assertEquals("Wall-E", movie.getTitle());	
			assertEquals("http://cdon.se/media-dynamic/images/product/00/00/91/75/28/3/bd1f6e97-5508-4805-a8de-4937347b1261.jpg", movie.getPosterURL());
		} else {
			assertNotNull("Always pass in offline mode");
		}

	}
	//Tests of getCDONPosterURL - only works in online mode
	public void testGetCdonPosterSuccessLarge() {
		if(!filmdeltaPlugin.isOffline()) {
			String posterUrl = filmdeltaPlugin.getCDONPosterURL("citizen kane", 0);
			assertEquals("http://cdon.se/media-dynamic/images/product/000/437/437517.jpg", posterUrl);
		}
	}
	public void testGetCdonPosterSuccessSmall() {
		if(!filmdeltaPlugin.isOffline()) {
			String posterUrl = filmdeltaPlugin.getCDONPosterURL("mora träsk cirkus", 0);
			assertEquals("http://cdon.se/media-dynamic/images/product/000/406/406535.jpg", posterUrl);
		}
	}
	public void testGetCdonPosterWithSeasonSuccess() {
		if(!filmdeltaPlugin.isOffline()) {
			String posterUrl = filmdeltaPlugin.getCDONPosterURL("dexter", 1);
			assertEquals("http://cdon.se/media-dynamic/images/product/000/533/533524.jpg", posterUrl);
		}
	}
	public void testGetCdonPosterMovieNotFound() {
		if(!filmdeltaPlugin.isOffline()) {
			String posterUrl = filmdeltaPlugin.getCDONPosterURL("apo panda", 0);
			assertEquals("UNKNOWN", posterUrl);
		}
	}
	public void testGetCdonPosterNotFound() {
		if(!filmdeltaPlugin.isOffline()) {
			//TODO
		}
	}
	//Function tests - only work in offline mode
	public void testGetCdonMovieUrlSuccess() {
		if(filmdeltaPlugin.isOffline()) {
			filmdeltaPlugin.setRequestResult("<img class=\"icon\" src=\"/media-static/images/icon/section-movie.gif\" alt=\"\" /><h2>Film - 1 tr&#228;ff</h2><div class=\"right\"></div></div> " + 
					"<div class=\"section-shadow\"></div><div class=\"content-container\"><table class=\"product-list\" cellpadding=\"0\" cellspacing=\"0\"><tr><th colspan=\"5\">Filmtitel - 1 tr&#228;ff</th>" + 
					"</tr><tr><td class=\"format\"><img src=\"/media-dynamic/images/format/2-199-small.gif\" alt=\"DVD\" title=\"DVD\" /></td><td class=\"title\">" + 
				 	"<a href=\"http://cdon.se/film/dexter_-_s%c3%a4song_1_(4_disc)-704895\" rel=\"imagetooltip[%2fmedia-dynamic%2fimages%2fproduct%2f000%2f533%2f533526.jpg]\">Dexter - S&#228;song 1 (4 disc)</a></td><td class=\"date\">2008-02-27</td>");
			assertEquals("http://cdon.se/film/dexter_-_s%c3%a4song_1_(4_disc)-704895", filmdeltaPlugin.getCdonMovieUrl("dexter", 1));
		}
	}
	public void testGetCdonMovieUrlNotFound() {
		if(filmdeltaPlugin.isOffline()) {
			filmdeltaPlugin.setRequestResult("");
			assertEquals("UNKNOWN", filmdeltaPlugin.getCdonMovieUrl("test movie", 0));
		}
	}
	public void testGetCdonMovieDetailsPageSuccess() {
		if(filmdeltaPlugin.isOffline()) {
			filmdeltaPlugin.setRequestResult("<ett testresultat>");
			String result = filmdeltaPlugin.getCdonMovieDetailsPage("en förlorad värld", "http://cdon.se/film/en_f%c3%b6rlorad_v%c3%a4rld-4062472");
			assertEquals("<ett testresultat>", result.toString());
		}
	}
	public void testGetCdonMovieDetailsPageNotFound() {
		if(filmdeltaPlugin.isOffline()) {
			filmdeltaPlugin.setRequestResult("nourl found here");
			assertEquals("UNKNOWN", filmdeltaPlugin.getCdonMovieDetailsPage("kung fu panda", "nourl"));
		}
	}
	public void testExtractCdonPosterUrlSuccessLargeCover() {
		String cdonMoviePage = "<div class=\"product-image-container\">" + 
			"<a href=\"/media-dynamic/images/product/00/04/06/24/72/3/77cbe33d-3352-43b2-b460-4370582448df.jpg\" rel=\"imageviewer\"><img src=\"/media-dynamic/images/product/00/04/06/24/72/1/84f4f42c-87b8-4991-b4b7-ea6b1b8ff818.jpg\" alt=\"En F&#246;rlorad V&#228;rld\" class=\"product\" /></a>" + 
			"<p><a href=\"/media-dynamic/images/product/00/04/06/24/72/3/77cbe33d-3352-43b2-b460-4370582448df.jpg\" rel=\"imageviewer\">St&#246;rre framsida</a></p>";			
		filmdeltaPlugin.setRequestResult("");
		String result = filmdeltaPlugin.extractCdonPosterUrl("En Förlorad Värld", cdonMoviePage);
		assertEquals("http://cdon.se/media-dynamic/images/product/00/04/06/24/72/3/77cbe33d-3352-43b2-b460-4370582448df.jpg", result);
	}
	public void testExtractCdonPosterUrlSuccessSmallsCover() {
		String cdonMoviePage = "<div class=\"product-image-container\"><img src=\"/media-dynamic/images/product/000/406/406535.jpg\" alt=\"Mora Tr&#228;sk - Mora Tr&#228;sk P&#229; Cirkus\" class=\"product\" />";
		filmdeltaPlugin.setRequestResult("");
		String result = filmdeltaPlugin.extractCdonPosterUrl("Mora Träsk på Cirkus", cdonMoviePage);
		assertEquals("http://cdon.se/media-dynamic/images/product/000/406/406535.jpg", result);
	}
	public void testExtractCdonPosterUrlFail() {
		filmdeltaPlugin.setRequestResult("no cover found here");
		assertEquals("UNKNOWN", filmdeltaPlugin.extractCdonPosterUrl("test", "http://cdon.se"));
	}	
}
