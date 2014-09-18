package org.magnum.dataup;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.magnum.dataup.model.VideoStatus.VideoState.READY;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * Video service implementation.
 */
@Controller
public class VideoSvcImpl {
	/**
	 * Video service URL path (root).
	 */
	private static final String VIDEO_SVC_PATH = "/video";
	private static final String SEPARATOR = "/";
	private static final String DATA = "/data";
	/**
	 * Stored (not persistent) videos.
	 */
	private Map<Long, Video> videos = new HashMap<Long, Video>();
	/**
	 * ID sequence.
	 */
	private static final AtomicLong videoIdSequence = new AtomicLong(0L);

	/**
	 * @return lists of uploaded Videos.
	 */
	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Collection<Video> getVideoList() {
		return videos.values();
	}

	/**
	 * Saves the video metadata and returns ID plus Path URL information in the
	 * response.
	 * 
	 * @param entity
	 *            input video metadata.
	 * @return metadata extended with an unique ID and data path URL.
	 */
	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody
	Video addVideo(@RequestBody Video entity) {
		// set generated fields
		final long videoId = videoIdSequence.incrementAndGet();
		entity.setId(videoId);
		entity.setDataUrl(getUrlBaseForLocalServer() + VIDEO_SVC_PATH
				+ SEPARATOR + videoId + DATA);
		// add to videos
		videos.put(videoId, entity);
		return entity;
	}

	/**
	 * Tries to save video data for the video specified by the given id.
	 * 
	 * @param id
	 *            video id.
	 * @param file
	 *            video file needs to be saved.
	 * @param response
	 *            http response used to communicate error codes.
	 * @return video status (always ready in this version).
	 */
	@RequestMapping(value = VIDEO_SVC_PATH + SEPARATOR + "{id}" + DATA, method = RequestMethod.POST)
	public @ResponseBody
	VideoStatus setVideoData(@PathVariable(value = "id") long id,
			@RequestParam(value = "data") MultipartFile file,
			HttpServletResponse response) {
		try {
			final Video v = loadById(id);
			// store video
			VideoFileManager fileManager = VideoFileManager.get();
			fileManager.saveVideoData(v, file.getInputStream());
		} catch (IllegalArgumentException e) {
			response.setStatus(SC_NOT_FOUND); // video not found
		} catch (IOException ioEx) {
			response.setStatus(SC_INTERNAL_SERVER_ERROR); // internal error
															// during video save
		}
		return new VideoStatus(READY);
	}

	/**
	 * Tries to return the video data for the given id.
	 * 
	 * @param id
	 *            video id.
	 * @param response
	 *            http response used to communicate error codes.
	 */
	@RequestMapping(value = VIDEO_SVC_PATH + SEPARATOR + "{id}" + DATA, method = RequestMethod.GET)
	public void getData(@PathVariable(value = "id") long id,
			HttpServletResponse response) {
		try {
			final Video v = loadById(id);
			// return video
			VideoFileManager fileManager = VideoFileManager.get();
			if (fileManager.hasVideoData(v)) {
				// copy video to the response's output
				fileManager.copyVideoData(v, response.getOutputStream());
			} else {
				// no video found (not yet uploaded)
				response.setStatus(SC_NOT_FOUND);
			}
		} catch (IllegalArgumentException e) {
			response.setStatus(SC_NOT_FOUND);
		} catch (IOException ioEx) {
			response.setStatus(SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Tries to load video for the given id.
	 * 
	 * @param id
	 *            the id of the requested video.
	 * @return the video which belongs to the given id.
	 * @throws IllegalArgumentException
	 *             on invalid (not positive or non-existing) id.
	 */
	public Video loadById(long id) throws IllegalArgumentException {
		// not positive id
		if (id < 1)
			throw new IllegalArgumentException("Id must be positive");
		// retrieve video
		Video video = videos.get(id);
		// no entry found for the specified id
		if (video == null)
			throw new IllegalArgumentException(
					"No entry found for the specified id");
		return video;
	}

	/**
	 * @return local server URL base.
	 */
	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
