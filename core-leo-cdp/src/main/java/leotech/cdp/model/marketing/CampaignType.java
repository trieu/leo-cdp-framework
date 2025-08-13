package leotech.cdp.model.marketing;

/**
 * all types of digital marketing campaign from USER_DEFINED, BRAND_AWARENESS to REFERRAL_PROGRAM
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class CampaignType {

	/**
	 * manually define the automated flow
	 */
	public static final int USER_DEFINED = 0; // manually

	/**
	 * Search Engine Optimization (SEO): <br>
	 * This involves optimizing your website and online content to rank higher in
	 * search engine resultspages (SERPs) for relevant keywords. This increases
	 * brand visibility and organic traffic to your website. 
	 * <br> <br>
	 * Social Media Marketing (SMM):<br>
	 * Creating engaging content and interacting with your target audience on social
	 * media platforms like Facebook, Instagram, and Twitter helps build brand
	 * awareness and establish your brand voice. 
	 * <br> <br>
	 * Content Marketing: <br>
	 * Creating valuable and informative content, such as blog posts, infographics,
	 * and videos, that educates and entertains your target audience, positions you
	 * as a thought leader in your industry and builds brand awareness. 
	 * <br> <br>
	 * Influencer Marketing: <br>
	 * Partnering with social media influencers who have a large and engaged
	 * following in your target market can help you reach a wider audience and
	 * increasebrand awareness.
	 */
	public static final int BRAND_AWARENESS = 1;// in-bound marketing to attract new visitors

	/**
	 * in-bound marketing to attract new visitors. 
	 * <br><br>
	 * Content Marketing: <br>
	 * Offering gated content, such as ebooks, white papers, or webinars, in
	 * exchange for user contact information helps you generate leads.
	 *  <br>  <br>
	 * Social Media Advertising: Running targeted ads on social media platforms
	 * allows you to reach a highly specific audience with your message and generate
	 * leads.
	 *  <br> <br>
	 * Email Marketing:<br>
	 * Building an email list and sending regular newsletters or promotional emails
	 * is a great way to nurture leads and convert them into customers.
	 */
	public static final int LEAD_GENERATION = 2;//

	/**
	 * Pay-per-Click (PPC) Advertising: <br>
	 * You can use PPC advertising to drive conversions by directing users to
	 * landing pages designed to convert them into customers, such as product pages
	 * or signup forms. 
	 * <br><br>
	 * Retargeting Campaigns: <br>
	 * These campaigns target users who have previously visited your website or
	 * interacted with your brand online. They can be a very effective way to remind
	 * users about your products or services and encourage them to convert.
	 */
	public static final int CONVERSION_OPTIMIZATION = 3;

	/**
	 * Email Marketing: <br>
	 * Regular email communication with your customers helps you stay top-of-mind
	 * and build relationships. You can use email marketing to send targeted
	 * campaigns based on purchase history or interests, or to promote loyalty
	 * programs and exclusive offers. 
	 * <br> <br>
	 * Social Media Engagement: <br>
	 * Continuing to interact with your followers on social media after they have
	 * purchased from you helps you build relationships and encourage repeat
	 * business.
	 * 
	 */
	public static final int RETENTION_OPTIMIZATION = 4;

	/**
	 * cross-sales for similar product offering
	 */
	public static final int CROSS_SELLING = 5;

	/**
	 * Combo Package of products and services tp up-sell and increase profit and
	 * cash flow
	 */
	public static final int UPSELLING = 6;

	/**
	 * Referral Programs: <br>
	 * Encourage existing customers to refer their friends and
	 * family by offering them rewards or discounts. This is a great way to acquire
	 * new customers and build brand loyalty. Using CDP short URL tool, you can
	 * share posts with referral links to products in social media
	 */
	public static final int REFERRAL_PROGRAM = 7;

}
