package leotech.cdp.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.ProcessedContent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.marketing.TargetMediaUnit;

/**
 * Activation Flow Management
 *
 * Responsibilities:
 * - Build email content
 * - Trigger email / push via gateway
 * - Handle activation tracking
 */
public class EmailFlowManagement {



    // ============================================================
    // Activation / Content Processing
    // ============================================================

    static ProcessedContent processContent(
            AssetTemplate template,
            Map<String, Object> context
    ) {
        try {
            Handlebars handlebars = new Handlebars();
            Template titleTpl =
                    handlebars.compileInline(template.getTitle());
            Template contentTpl =
                    handlebars.compileInline(template.getMediaInfo());

            return new ProcessedContent(
                    titleTpl.apply(context),
                    contentTpl.apply(context)
            );
        } catch (IOException e) {
            throw new RuntimeException("Template processing failed", e);
        }
    }

    public static boolean recommendProductItems(
            Profile profile,
            AssetTemplate assetTemplate,
            List<ProductItem> productItems
    ) {
        if (productItems.isEmpty()) {
            return false;
        }

        ProductItem item = productItems.get(0);

        TargetMediaUnit target =
                new TargetMediaUnit(
                        item.getInCampaigns().iterator().next(),
                        item.getId(),
                        item.getHeadlineImageUrl(),
                        item.getHeadlineVideoUrl(),
                        profile.getId(),
                        item.getFullUrl(),
                        item.getTitle(),
                        EventMetricManagement
                                .getEventMetricByName("email-click")
                                .getId()
                );

        TargetMediaUnitDaoUtil.save(target);

        Map<String, Object> context = new HashMap<>();
        context.put("profile", profile);
        context.put("productItems", productItems);
        context.put("targetMediaUnit", target);

        return !processContent(assetTemplate, context).isEmpty();
    }

    public static boolean recommendContentItems(
            Profile profile,
            AssetTemplate assetTemplate,
            List<AssetContent> contentItems
    ) {
        Map<String, Object> context = new HashMap<>();
        context.put("profile", profile);
        context.put("contentItems", contentItems);

        return !processContent(assetTemplate, context).isEmpty();
    }
}
