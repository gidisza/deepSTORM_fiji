
package mpicbg.csbd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.scijava.io.http.HTTPLocation;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.module.MethodCallException;

public class IOHelper {

	public static Location loadFileOrURL(final String path)
		throws FileNotFoundException
	{
		if (path == null) {
			throw new FileNotFoundException("No path specified");
		}
		final File file = new File(path);
		Location source;
		if (!file.exists()) {
			try {
				source = new HTTPLocation(path);
			}
			catch (MalformedURLException | URISyntaxException exc) {
				throw new FileNotFoundException("Could not find file or URL: " + path);
			}
		}
		else {
			source = new FileLocation(file);
		}
		return source;

	}

	public static boolean urlExists(String url) {
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("HEAD");
			return con.getResponseCode() == HttpURLConnection.HTTP_OK;
		} catch (IOException | IllegalArgumentException e) {
			return false;
		}
	}

}
