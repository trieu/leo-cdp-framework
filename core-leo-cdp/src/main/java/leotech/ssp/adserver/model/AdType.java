package leotech.ssp.adserver.model;

public interface AdType {

	// BEGIN primary ad types
	public static final int ADTYPE_TAGGING_AD = 0;
	
	// standard VAST video Ad: Preroll, Midroll, Postroll
	public static final int ADTYPE_INSTREAM_VIDEO = 1;
	
	// Interactive HTML5 Overlay on Video
	public static final int ADTYPE_EXPANDABLE_OVERLAY = 2;
	
	// Static Image Overlay on Video
	public static final int ADTYPE_IMAGE_OVERLAY = 3;
	
	// Interactive Text Overlay on Video
	public static final int ADTYPE_BREAKING_NEWS_OVERLAY = 4;
									
	// standard display HTML5 banner:HTML5 Interactive Ad 
	public static final int ADTYPE_HTML5_DISPLAY_AD = 5;
														 
	// standard display Image banner: jpeg, gif, png
	public static final int ADTYPE_IMAGE_DISPLAY_AD = 6;
	
	// Inread,Inpage with flexible size
	public static final int ADTYPE_OUTSTREAM_RICH_MEDIA = 7; 
	
	// Native sponsored content in Content Marketing
	public static final int ADTYPE_SPONSORED_STORY_AD = 8;
	
	// Ad Code of 3rd party
	public static final int ADTYPE_BIDDING_AD = 9; 
	
	// Instream Video in Live TV
	public static final int ADTYPE_STREAMING_VIDEO_AD = 10;
	
	// masthead
	public static final int ADTYPE_MASTHEAD_AD = 11;
	
	// infeed
	public static final int ADTYPE_INFEED_AD = 12;
	
	// BALLOON ads
	public static final int ADTYPE_OUTSTREAM_BALLOON = 13;
	
	// video ads VAST wrapper
	public static final int ADTYPE_INSTREAM_VAST_WRAPPER = 14;
}
