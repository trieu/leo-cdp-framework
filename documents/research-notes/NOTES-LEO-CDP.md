
# LEO CDP

## Logo 

logomakr.com/9qp3SD

## Cost & Pricing 

* One year subscription on private Google cloud: $960
* Monthly Product package = $999 (ArangoDB + Google Cloud)
* Total monthly infrastructure cost: $64 (e2-medium (2 vCPUs, 4 GB memory)) + 
* Total monthly DevOps cost: $89.33
* Total monthly consulting and training cost: $100
* Total monthly profit: $200

ArangoDB cost: 8 GB	20 GB (or more)	$ 0.52/hr at https://cloud.arangodb.com/home

Cloud compute cost: https://cloud.google.com/products/calculator/#id=1f054b6b-d785-4f8d-b8ea-145a0127c4fb

## Cost for On-premise Platform Deployment in Vietnam for Retail Industry

* (5000000 - 2,781,600) / 5000
* License cost for each profile = $0.12
* Cost details for Amazon https://calculator.aws/#/estimate?id=7a610640c1dd17871c6342c6c670be9044102eaa
* One year of AWS = 1500 USD, Total monthly = 120 USD
* if a company have 20,000 contact profiles, 5,000 first profile is free, total cost for one year = $1,800
=> Total cost = cloud + license + email marketing = $1500 + $1,800 + $300= $3,600 / year  = 83,439,000 VND

## Slide

* [Product vision](https://docs.google.com/presentation/d/1GHUXBjdUctDxkUS_WLqls_aT79GqOlYN3MIQvXwLN5s/edit?usp=sharing)
* Flow https://github.com/USPA-Technology/leotech-final-build/blob/master/leo-cdp-event-observer-data-flow.md

## Ref products

* https://support.dotdigital.com/hc/en-gb/categories/360002299859-Ecommerce
* https://www.relateddigital.com/en/customer-data-platform/
* ActionIQ Financial Services Customer: Genworth Financial https://vimeo.com/453792039
* Martech Mastery: Key Differences Between CDPs & MDMs https://vimeo.com/631150742

## Case study 

## Smart Recommendation Engine with multiple algorithms

https://dzone.com/articles/building-sales-recommendation-engine-with-apache-s
https://download.oracle.com/otndocs/products/spatial/pdf/oow_2016/OW2016_Building_a_Java_Recommender_System_in_15_Minutes_with_Graph_Technologies.pdf

## Public API for tracking

getContextSession
http://demotrack.leocdp.net/css-init?observer=tester&media=xemgiday.com&itouchid=homepage&visid=12234

record-view-event
http://demotrack.leocdp.net/etv?observer=tester&media=xemgiday.com&itouchid=homepage&visid=12234&ctxsk=1NlAlT5gPD4Oru3c8jVKZt&en=pageview&stouchid=homepage

record-action-event
http://demotrack.leocdp.net/eta?observer=tester&media=xemgiday.com&itouchid=homepage&visid=12234&ctxsk=1NlAlT5gPD4Oru3c8jVKZt&en=click&stouchid=homepage

record-conversion-event
http://demotrack.leocdp.net/eta?observer=tester&media=xemgiday.com&itouchid=homepage&visid=12234&ctxsk=1NlAlT5gPD4Oru3c8jVKZt&en=add_to_cart&stouchid=homepage

## Core data models
### Version 1.0

1. Touchpoint => TouchpointDaoUtil (list, save, getById) => TestTouchpointDaoUtil
1. EventObserver => EventObserverDaoUtil (list, save, getById) => TestEventObserverDaoUtil

## Security

* https://howtodoinjava.com/security/aes-256-encryption-decryption/

## Observer(Tag) Management for event tracking
* learn from https://tealium.com/assets/pdf/tag_management_comparison_matrix.pdf
* https://community.tealiumiq.com/t5/Data-Layer/Data-Layer-Definition-Publisher/ta-p/17268
* https://docs.tealium.com/platforms/javascript/universal-data-object/

## Restful API for data query
* https://www.youtube.com/watch?v=sAVJDyQd4j4
* https://github.com/TechPrimers/restwebservice-vertx-example
* https://www.eclipse.org/community/eclipse_newsletter/2016/october/article4.php
* https://vertx.io/docs/vertx-auth-jwt/java/

## Chart & Visualization

* Profile GRAPH libs:  https://blog.js.cytoscape.org/2016/05/24/getting-started/ 
* http://sigmajs.org
* https://www.chartjs.org
* Cohort Visualization library https://github.com/restorando/cornelius
* https://github.com/kiransunkari/retention-graph-using-D3-cohort-analysis-
* User Journey, Gantt, Sequence https://github.com/mermaid-js/mermaid
* A dynamic, reusable donut chart for D3.js v4 https://bl.ocks.org/mbhall88/22f91dc6c9509b709defde9dc29c63f2
* https://tobiasahlin.com/blog/chartjs-charts-to-get-you-started/#2-line-chart
* Doughnut chart using Chart.js https://jsfiddle.net/cmyker/ooxdL2vj/
* https://www.npmjs.com/package/chartjs-gauge

## Profile UI/UX
* Ref https://piwik.pro/customer-data-platform/
* Ref https://www.hull.io/
* infinite scrolling for event stream https://jscroll.com/#/
* https://richpanel.com/identity-resolution/

## CX system

* Bootstrap 4 listing rating and review template with scoreboard https://bbbootstrap.com/snippets/bootstrap-listing-rating-and-review-template-scoreboard-16284505
* Bootstrap 4 rating star with inputs using jquery https://bbbootstrap.com/snippets/bootstrap-rating-star-inputs-using-jquery-22603699
* Bootstrap 4 animated rating stars https://bbbootstrap.com/snippets/animated-rating-stars-18298447

## Account management for B2B
* Trello UI Demo (jQuery Sortable) https://codepen.io/nnamdi43/pen/oExKMj
* https://js.plainenglish.io/using-javascript-to-create-trello-like-card-re-arrange-and-drag-and-drop-557e60125bb4

## AI for Marketing
* https://towardsdatascience.com/customer-preferences-in-the-age-of-the-platform-business-with-the-help-of-ai-98b0eabf42d9

# NPS CX Report 
* https://www.checkmarket.com/blog/net-promoter-score/
* https://www.surveysensum.com/customer-experience/net-promoter-score/calculate-net-promoter-score/

## Cloud Native 
* https://www.youtube.com/watch?v=ZjJtxq3Oju8

## Case Studies 
* Introduction to LEO CDP Webinar https://docs.google.com/document/d/1la6mP21gfd2bHlpwj4hBTRQlxaPfhnpQRL6fV223Es0/edit?usp=sharing
* Retail https://drive.google.com/file/d/1St_5GSdVajM0CrNFiROqjn2Q_jgo4o6w/view?usp=sharing
* https://blog.thunderquote.com/2019/06/07/10-tips-increase-email-marketing-sales/
* https://blog.thunderquote.com/2019/06/03/tech-trends-digital-marketing/

## TODO
* monthly Lead Cost CPL estimation (market, media inventory, competitor, creative)
* 

## Links for Pitch Deck 





