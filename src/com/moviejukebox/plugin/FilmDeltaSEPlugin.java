/* Filmdelta.se plugin
 * 
 * Contains code for an alternate plugin for fetching information on 
 * movies in swedish
 * 
 */

package com.moviejukebox.plugin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;


/**
 * Plugin to retrieve movie data from Swedish movie database www.filmdelta.se
 * Modified from imdb plugin and Kinopoisk plugin written by Yury Sidorov.
 * 
 * @author  johan.klinge
 * @version 0.4, 12th February 2009
 */
public class FilmDeltaSEPlugin extends ImdbPlugin {

    public static String FILMDELTA_PLUGIN_ID = "filmdelta";
    protected TheTvDBPlugin tvdb;
    
    //Get properties for plotlength and rating
    int preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("filmdelta.plot.maxlength", "400"));
    String preferredRating = PropertiesUtil.getProperty("filmdelta.rating", "filmdelta");
    String getcdonposter = PropertiesUtil.getProperty("filmdelta.getcdonposter", "true");

    public FilmDeltaSEPlugin() {
        super();
        tvdb = new TheTvDBPlugin(); 
        logger.finest("Filmdelta plugin created..");
    }

    @Override
    public boolean scan(Movie mediaFile) {
    	
        boolean retval = true;
        String filmdeltaId = mediaFile.getId(FILMDELTA_PLUGIN_ID);
        String imdbId = mediaFile.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        boolean imdbScanned = false; 
        
        // if IMDB id is specified in the NFO scan imdb first
        // (to get a valid movie title and improve detection rate 
        // for getFilmdeltaId-function)
        if (imdbId != null && !imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
        	super.scan(mediaFile);
        	imdbScanned = true;
        }
        
        if (filmdeltaId == null || filmdeltaId.equalsIgnoreCase(Movie.UNKNOWN)) { 
        	//find a filmdeltaId (url) from google
        	filmdeltaId = getFilmdeltaId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.getSeason());
            if (!filmdeltaId.equalsIgnoreCase(Movie.UNKNOWN)) {
            	mediaFile.setId(FILMDELTA_PLUGIN_ID, filmdeltaId);
            }
        } else {
        	// If ID is specified in NFO, set original title to unknown
        	mediaFile.setTitle(Movie.UNKNOWN);
        }
        
        //scrape info from imdb or tvdb
    	if (mediaFile.isTVShow()) {
    		tvdb.scan(mediaFile);
    	} else if (!imdbScanned)  {
    		super.scan(mediaFile);
        }
        
        //only scrape filmdelta if a valid filmdeltaId was found
        //and the movie is not a tvshow
        if (filmdeltaId != null 
        		&& !filmdeltaId.equalsIgnoreCase(Movie.UNKNOWN) 
        		&& !mediaFile.isTVShow()) {
        	retval = updateFilmdeltaMediaInfo(mediaFile, filmdeltaId);
        }
        
        // Get poster from CDON.se
        // if property getcdonposter is set to true
        if (getcdonposter.equalsIgnoreCase("true")) {
        	String posterURL = getCDONPosterURL(mediaFile.getTitle(), mediaFile.getSeason());
        	if (!posterURL.equals(Movie.UNKNOWN)) {
        		mediaFile.setPosterURL(posterURL);
            }	
        }
        return retval;
    }

    /* Find id from url in nfo. Format:
     *  - http://www.filmdelta.se/filmer/<digits>/<movie_name>/ OR
     *  - http://www.filmdelta.se/prevsearch/<text>/filmer/<digits>/<movie_name>
     */
    @Override
    public void scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);
        logger.finest("Scanning NFO for Filmdelta Id");
        
        int beginIndex = nfo.indexOf("www.filmdelta.se/prevsearch");
        if (beginIndex != -1) {
        	beginIndex = beginIndex + 27;
            String filmdeltaId = makeFilmDeltaId(nfo, beginIndex, 2);
            movie.setId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID, filmdeltaId);
            logger.finest("Filmdelta Id found in nfo = " + movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
        } else if (nfo.indexOf("www.filmdelta.se/filmer") != -1) {
        	beginIndex = nfo.indexOf("www.filmdelta.se/filmer") + 24;
        	String filmdeltaId = makeFilmDeltaId(nfo, beginIndex, 0);
            movie.setId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID, filmdeltaId);
            logger.finest("Filmdelta Id found in nfo = " + movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
        } else {  
        	logger.finer("No Filmdelta Id found in nfo!");
        }
    }

     /**
     * retrieve FilmDeltaID matching the specified movie name and year. 
     * This routine is based on a  google request.
     */
    protected String getFilmdeltaId(String movieName, String year, int season) {
    	try {
            StringBuffer sb = new StringBuffer("http://www.google.se/search?hl=sv&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+").append(year);
            }
            sb.append(URLEncoder.encode("+site:filmdelta.se/filmer", "UTF-8"));
            String googleHtml = webBrowser.request(sb.toString());
            
            //String <ul><li> is only present in the google page for
            //no matches so check if we got a page with results
            if (googleHtml.indexOf("<ul><li>") ==  -1) {
            	//we have a a google page with valid filmdelta links
            	int beginIndex = googleHtml.indexOf("www.filmdelta.se/filmer/") + 24;
            	String filmdeltaId = makeFilmDeltaId(googleHtml, beginIndex, 0);
                logger.finest("FilmdeltaID = " + filmdeltaId);
                if (filmdeltaId.matches("\\d{3,}/[\\w-&;]+")) {
                    return filmdeltaId;
                } else {
                    logger.info("FilmDeltaSEPlugin: found a filmdeltaId but it's not valid. Id: " + filmdeltaId);
                	return Movie.UNKNOWN;
                }
            } else {
            	//no valid results for the search
            	logger.info("No filmdelta.se matches found for movie: \'" + movieName + "\'");
            	return Movie.UNKNOWN;	     	
            }

            
        } catch (Exception e) {
            logger.severe("Failed retreiving Filmdelta Id for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }
    
    /* 
     * Utility method to make a filmdelta id from a string containing a 
     * filmdelta url
     */
	private String makeFilmDeltaId(String nfo, int beginIndex, int skip) {
		StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex), "/");
		for (int i = 0; i < skip; i++) {
			st.nextToken();	
		}
		String filmdeltaId = st.nextToken() + "/" + st.nextToken();
		return filmdeltaId;
	}

    /*
     * Scan Filmdelta html page for the specified movie
     */
    protected boolean updateFilmdeltaMediaInfo(Movie movie, String filmdeltaId) {
    	
    	String fdeltaHtml = "";
    	//fetch filmdelta html page for movie
        fdeltaHtml = getFilmdeltaHtml(filmdeltaId);
        
        if (!fdeltaHtml.equals(Movie.UNKNOWN)) {
        	getFilmdeltaTitle(movie, fdeltaHtml);    
            getFilmdeltaPlot(movie, fdeltaHtml);
    		//Genres - prefer imdb
            if (movie.getGenres().isEmpty()) {
    			getFilmdeltaGenres(movie, fdeltaHtml);
    		}
           	getFilmdeltaDirector(movie, fdeltaHtml);
            getFilmdeltaCast(movie, fdeltaHtml);
            getFilmdeltaCountry(movie, fdeltaHtml);
            getFilmdeltaYear(movie, fdeltaHtml);    
            getFilmdeltaRating(movie, fdeltaHtml);
            getFilmdeltaRuntime(movie, fdeltaHtml);	
        }
        return true;
    }

	private String getFilmdeltaHtml(String filmdeltaId) {
		String result = Movie.UNKNOWN;
		try {
        	logger.finest("searchstring: " + "http://www.filmdelta.se/filmer/" + filmdeltaId);
            result = webBrowser.request("http://www.filmdelta.se/filmer/" + filmdeltaId + "/");
            logger.finest("result from filmdelta: " + result);
            
        } catch (Exception e) {
            logger.severe("Failed retreiving movie data from filmdelta.se : " + filmdeltaId);
            e.printStackTrace();
        }
		return result;
	}
	
    private void getFilmdeltaTitle(Movie movie, String fdeltaHtml) {
		if (!movie.isOverrideTitle()) {
			String newTitle = HTMLTools.extractTag(fdeltaHtml, "title>", 0, "<");
			//split the string so that we get the title at index 0
			String[] titleArray = newTitle.split("-\\sFilmdelta");
			if (titleArray != null) {
				newTitle = titleArray[0];
			} else {
				logger.finer("Error scraping title");
			}
			String originalTitle = HTMLTools.extractTag(fdeltaHtml, "riginaltitel</h4>", 2);
			logger.finest("Scraped title: " + newTitle);
			logger.finest("Scraped original title: " + originalTitle);
			if (!newTitle.equals(Movie.UNKNOWN)) {
		        movie.setTitle(newTitle.trim());
			}
			if (!originalTitle.equals(Movie.UNKNOWN)) {
				movie.setOriginalTitle(originalTitle);
			}
		}
	}

	private void getFilmdeltaPlot(Movie movie, String fdeltaHtml) {
		String plot = HTMLTools.extractTag(fdeltaHtml, "<div class=\"text\">", 2);
		logger.finest("Scraped Plot: " + plot);
		if (!plot.equals(Movie.UNKNOWN)) {
			if (plot.length() > preferredPlotLength) { 
				plot = plot.substring(0, preferredPlotLength) + "...";
			}
			movie.setPlot(plot);
		}
	}
    
	private void getFilmdeltaGenres(Movie movie, String fdeltaHtml) {
		LinkedList<String> newGenres = new LinkedList<String>();            
		
		ArrayList<String> filmdeltaGenres = HTMLTools.extractTags(fdeltaHtml, "<h4>Genre</h4>", "</div>", "<h5>", "</h5>");
		for (String genre : filmdeltaGenres) {
			if (genre.length() > 0) {
				genre = genre.substring(0, genre.length() - 5);
				newGenres.add(genre);
			}
		}           
		if (!newGenres.isEmpty()) {
			movie.setGenres(newGenres);
			logger.finest("Scraped genres: " + movie.getGenres().toString());
		}
	}
    
	private void getFilmdeltaDirector(Movie movie, String fdeltaHtml) {
		ArrayList<String> filmdeltaDirectors = HTMLTools.extractTags(fdeltaHtml, "<h4>Regiss&ouml;r</h4>", "</div>", "<h5>", "</h5>");
		String newDirector = "";
		if (!filmdeltaDirectors.isEmpty()) {
			for (String dir : filmdeltaDirectors) {
				dir = dir.substring(0, dir.length() - 4);
				newDirector = newDirector + dir + " / ";
			}
			newDirector = newDirector.substring(0, newDirector.length() - 3);
			movie.setDirector(newDirector);
			logger.finest("Scraped director: " + movie.getDirector());	
		}
	}
    
	private void getFilmdeltaCast(Movie movie, String fdeltaHtml) {
		Collection<String> newCast = new ArrayList<String>();
         
		for (String actor : HTMLTools.extractTags(fdeltaHtml, "<h4>Sk&aring;despelare</h4>", "</div>", "<h5>", "</h5>")) {
			String[] newActor = actor.split("</a>");
			newCast.add(newActor[0]);
		}
		if (newCast.size() > 0) { 
			movie.setCast(newCast);
			logger.finest("Scraped actor: " + movie.getCast().toString());
		}
	}
    
    private void getFilmdeltaCountry(Movie movie, String fdeltaHtml) {
		String country = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 3);
		movie.setCountry(country);
		logger.finest("Scraped country: " + movie.getCountry());
	}
    
	private void getFilmdeltaYear(Movie movie, String fdeltaHtml) {
        String year = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 5);
        String[] newYear = year.split("\\s");
        if (newYear.length > 1) {
        	movie.setYear(newYear[1]);
            logger.finest("Scraped year: " + movie.getYear());	
        } else {
        	logger.finer("Error scraping year for movie: " + movie.getTitle());
        }
	}
    
	private void getFilmdeltaRating(Movie movie, String fdeltaHtml) {
		String rating = HTMLTools.extractTag(fdeltaHtml, "style=\"margin-top:2px; font-weight:bold;\">", 8, "<");
		int newRating = 0;
		//check if valid rating string is found
		if (rating.indexOf("Snitt") != -1) {
			String[] result = rating.split(":");
			rating = result[result.length - 1];
			logger.finest("filmdelta rating: " + rating);
			//multiply by 20 to make comparable to IMDB-ratings
			newRating = (int) (Float.parseFloat(rating) * 20);
		} else {
			logger.finer("Error finding filmdelta rating");
		}
		//set rating depending on property value set by user
		if (preferredRating.equals("filmdelta")) {
			//fallback to imdb if no filmdelta rating is available
			if (newRating != 0) {
				movie.setRating(newRating);
			} else {
				logger.finer("Found no filmdelta rating. Using imdb."); 
			}
		} else if (preferredRating.equals("average")) {
			//don't count average rating if filmdelta has no rating
			if (newRating != 0) {
				newRating = (newRating + movie.getRating()) / 2;
				movie.setRating(newRating);
			} else {
				logger.finer("Found no filmdelta rating, no average calculation done. Using imdb rating");
			}
		   	
		}
		logger.finest("Movie.getRating: " + movie.getRating());
	}
    
	private void getFilmdeltaRuntime(Movie movie, String fdeltaHtml) {
		// Run time
        String runtime = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 7);
        String[] newRunTime = runtime.split("\\s");
        if (newRunTime.length > 2) {
        	movie.setRuntime(newRunTime[1]);
        	logger.finest("Scraped runtime: " + movie.getRuntime());            	
        }
	}

	protected String getCDONPosterURL(String movieName, int season) {
		String movieURL = Movie.UNKNOWN;
		String cdonMoviePage = Movie.UNKNOWN;
		String cdonPosterURL = Movie.UNKNOWN;
		
		//search CDON to find the url for the movie details page
		movieURL = getCdonMovieUrl(movieName, season);
		//then fetch the movie detail page
		cdonMoviePage = getCdonMovieDetailsPage(movieName, movieURL);		
		//extract poster url and return it
		cdonPosterURL = extractCdonPosterUrl(movieName, cdonMoviePage);
		return cdonPosterURL; 
	}

	protected String getCdonMovieUrl(String movieName, int season) {
		String html = Movie.UNKNOWN; 
		String movieURL = Movie.UNKNOWN;
		
		//Search CDON to get an URL to the movie page        	
		try {
			StringBuffer sb = new StringBuffer("http://cdon.se/search?q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8")); 
			if(season != 0) {
				sb.append("+").append(URLEncoder.encode("säsong", "UTF-8"));
				sb.append("+" + season);
			}
			html = webBrowser.request(sb.toString());
			//find the movie url in the search result page
			if (html.contains("/section-movie.gif\" alt=\"\" />")) {
				int beginIndex = html.indexOf("/section-movie.gif\" alt=\"\" />")+28;
				movieURL = HTMLTools.extractTag(html.substring(beginIndex), "<td class=\"title\">", 0);
				logger.finest("Found movieURL: " + movieURL);
			} else {
				movieURL = Movie.UNKNOWN;
				logger.finer("Error finding movieURL..");
			}
			return movieURL;
		}
		catch (Exception e) {
            logger.severe("Error while retreiving CDON image for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
	}
	protected String getCdonMovieDetailsPage(String movieName, String movieURL) {	
		String cdonMoviePage;
    	try {
    		//sanity check on result before trying to load details page from url
            if (!movieURL.isEmpty() && movieURL.contains("http")) {
            	//Split string to extract the url
            	String[] splitMovieURL = movieURL.split("\\s");
            	movieURL = splitMovieURL[1].replaceAll("href|=|\"", "");
            	logger.finest("found filmurl = " + movieURL);

            	//fetch movie page from cdon
            	StringBuffer buf = new StringBuffer(movieURL);
            	cdonMoviePage = webBrowser.request(buf.toString());
            	return cdonMoviePage;
            } else {
            	//search didn't even find an url to the movie
            	logger.finer("Error in fetching movie detail page from CDON for movie: " + movieName);
            	return Movie.UNKNOWN;
            }    		
    	} catch (Exception e) {
    		logger.severe("Error while retreiving CDON image for movie : " + movieName);
            //logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
    	}
	}
	protected String extractCdonPosterUrl(String movieName, String cdonMoviePage) {
		
		String cdonPosterURL = Movie.UNKNOWN;
		String[] htmlArray = cdonMoviePage.split("<");
		
		//check if there is an large front cover image for this movie
		if (cdonMoviePage.contains("St&#246;rre framsida")) {
			cdonPosterURL = findUrlString("St&#246;rre framsida", htmlArray);
		} else if(cdonMoviePage.contains("/media-dynamic/images/product/")){
			cdonPosterURL = findUrlString("/media-dynamic/images/product/", htmlArray);
		} else {
			logger.info("No CDON cover was found for movie: " + movieName);
		}
		return cdonPosterURL;
	}

	private String findUrlString(String searchString, String[] htmlArray) {
		String result;
		String[] posterURL = null;
		//all cdon pages don't look the same so
		//loop over the array to find the string with a link to an image
		int i = 0;
		for (String s : htmlArray) {
			if (s.contains(searchString)) {
				//found a matching string
				posterURL = htmlArray[i].split("\"|\\s");
				break;
			}
			i++;
		}            	
		//sanity check again (the found url should point to a jpg
		if (posterURL.length > 2 || posterURL[2].contains(".jpg")) {
			result = "http://cdon.se" + posterURL[2];
			logger.finest("Found large cover: " + result);
			return result; 
		} else {
			return Movie.UNKNOWN;
		}
	}
	
}
