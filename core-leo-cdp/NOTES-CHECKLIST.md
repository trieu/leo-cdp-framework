# Checklist for LEO CDP final 1.0 release

## Dev Logs 

https://docs.google.com/presentation/d/1wZDEjn0FfSzujcuEw6Y7xjtqRmatsdJjx-46atQKLUk/edit?usp=sharing

## Checklist Unified Analytics

1. ~~ Review the model for daily data reporting ~~
2. Daily Page-view and unique visitor report: https://stackoverflow.com/questions/51410379/cant-generate-multi-axis-combo-chart-bar-line-using-chart-js-2-7
3. Summary CX report
4. Export data into Pandas and visualize data, https://towardsdatascience.com/get-interactive-plots-directly-with-pandas-13a311ebf426
5. Export Data API into Jupyter notebook  https://www.trymito.io/
6. Apply RFM model https://github.com/USPA-Technology/crm-rfm-modeling

## Checklist Profile Tasks (13/02/2021 - 14/02/2021)

1. ~~ Fix bugs : no purchasing event, but Funnel Stage = repeat-customer ~~
2. ~~ Show data in the tab "Interested Items" for Profile Info ~~
3. ~~ Show data in the tab "Purchased Items" for Profile Info ~~
4. Visual Data Flow of Profile
5. Show data (Time Series Event Chart, Keyword trends, Touch-points Summary, Session Summary) in the tab "Data Insights" for Profile Info


## Checklist Data Activation Tasks (14/02/2021 - 15/02/2021)

1. Send email in profile and segment
2. Schedule to send email in segment
1. Manage Data Activation Service Model with Back-end System: handler and database
2. Embeddable web widget (JS Tag) of LEO Recommender System
3. Email Newsletter Template for Marketing Automation to recommend offered product

## Checklist LEO CDP API Tasks (16/02/2021 - 17/02/2021)

1. Add "JSON Web Token" for LEO CDP API
2. Secured API for LEO Event Observer 
3. Secured API for Profile Model
4. Secured API for Segment Model
5. Scheduled Data Activation Job with Python

# Checklist for deadline "final coding" on 17/02/2021

1. User Story Flow from feature matrix must be done, as Excel file
2. Unit Test (profile, tracking, segmentation and synch data jobs)
3. Sample data (1000 profiles and 5 segments) for demo 
4. Product Introduction slide
5. Quick Demo Video in 5 minutes
6. Install LEO CDP in AWS for USPA company 

# Checklist for deadline "final build" on 18h 18/02/2021

1. Local docker image to test
2. Binary jar: https://github.com/USPA-Technology/leo-cdp-binary-build
3. Docker image: https://hub.docker.com/r/uspa/leo-cdp
4. Simple installation guide in PDF
5. How to install video in 12 minutes 

# Checklist for deadline "public release" on 3h12 19/02/2021

1. Scheduling a Facebook and LinkedIn post (page, group, blog) for public release
2. Demo login account on AWS cloud
3. A landing page to get lead info (email, phone, name, purpose, company, marketing budget, estimated profile, owned channels)


CDP Solution:

Data Unification:

1) Membership system (demographics, membership level)
2) Website (pages visited, classes viewed)
3) Class booking system (class preferences, attendance)
4) marketing software (open rates, click-throughs)

Customer Segmentation: The CDP creates segments based on:

1) Interests (yoga vs. strength training)
2) Membership status (New, active, lapsed)
3) Location (Specific gym branches)
4) Engagement level (website activity, workout frequency, LiveWell time usage)

Targeted Campaigns:  California Fitness uses these segments to:

1) Send personalized offers to lapsed members, tailored to their preferences.
2) Recommend relevant classes to members based on their workout history.
3) Promote new programs to interested segments using tailored messaging.

Measuring Results: The CDP connects campaign data with customer actions:
- Track redemption rates of targeted offers.
- Measure class booking increases based on promotions.



