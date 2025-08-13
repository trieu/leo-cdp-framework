package leotech.cdp.model.asset;

/**
 * Content Type is used to show right item editor
 * 
 * @author tantrieuf31
 * @since 2019
 */
public abstract class ContentType {
	
	/**
     * basic raw text
     */
    public static final int META_DATA = 0;

    /**
     * basic HTML text
     */
    public static final int HTML_TEXT = 1;

    /**
     * rich media info-graphics with HTML5 canvas (using HTML5 Zip File Uploader)
     */
    public static final int HTML_RICH_MEDIA = 2;

    /**
     * Word/Excel/PDF document
     */
    public static final int OFFICE_DOCUMENT = 3;

    /**
     * VIDEO: MP4 video file or embedded URL from YouTube, FaceBook, ..
     */
    public static final int VIDEO = 4;
    
    /**
     * MP3 file or embedded URL from SoundCloud, ..
     */
    public static final int AUDIO = 5;

    /**
     * HTML5 Slide
     */
    public static final int HTML_SLIDE_SHOW = 6;

    /**
     * HTML5 Epub ebook
     */
    public static final int EPUB_EBOOK = 7;
    
    /**
     * Image Slide show
     */
    public static final int IMAGE_SLIDESHOW = 8;  
    
    /**
     * Handlebars template: check https://handlebarsjs.com and https://github.com/jknack/handlebars.java
     */
    public static final int TEMPLATE = 9;
    
    /**
     * json data
     */
    public static final int JSON_DATA = 10;
    
    /**
     * web url
     */
    public static final int WEB_URL = 11;
    
    
    /**
     * markdown text
     */
    public static final int MARKDOWN = 12;
}
