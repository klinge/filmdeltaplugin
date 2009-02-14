package com.moviejukebox.plugin;

import java.io.IOException;
import java.net.URL;

import com.moviejukebox.tools.WebBrowser;

public class FilmDeltaSEPluginMock extends FilmDeltaSEPlugin {

	private String requestResult;
	private boolean offline = true;

	public FilmDeltaSEPluginMock() {
		//change implementation for webbrowser
		//if offline returns a fixed result
		//if not offlina calls the 'live' browser in the superclass
		webBrowser = new WebBrowser() {
			public String request(URL url) throws IOException {
				if (offline) {
					return getRequestResult();
				} else {
					return super.request(url);
				}
			}
		};
	}

	public boolean isOffline() {
		return offline;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}
	
	public String getRequestResult() {
		return requestResult;
	}

	public void setRequestResult(String requestResult) {
		this.requestResult = requestResult;
	}	
}
