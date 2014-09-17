package org.magnum.dataup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	/**
	 * ID sequence.
	 */
	private static final AtomicLong videoIdSequence = new AtomicLong(0L);

	/**
	 * @return lists of uploaded Videos.
	 */
	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
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
	public @ResponseBody Video addVideo(@RequestBody Video entity) {
		// set generated fields
		final long videoId = videoIdSequence.incrementAndGet();
		entity.setId(videoId);
		entity.setDataUrl(getUrlBaseForLocalServer() + VIDEO_SVC_PATH + SEPARATOR + videoId + DATA);
		// add to videos
		videos.put(videoId, entity);
		return entity;
	}
	
	@RequestMapping(value = VIDEO_SVC_PATH + SEPARATOR + "{id}" + DATA, method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable(value="id") long id, MultipartFile file) {
		Video video = videos.get(id);
		if (video == null)
			throw new Error("Invalid video ID");
		return new VideoStatus(VideoState.READY);
	}

	/**
	 * @return local server URL base.
	 */
	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
