-- S3-15: Engagement y Notification reciben las ofertas vigentes que se publicaron antes de que Catalog
-- tuviera relay. occurredAt es el inicio de la campaña, así Notification no avisa por ellas.
-- 2000 es CampaignZone.DISTRICT_COVERAGE_METERS; una zona sin centro no tiene alcance.
INSERT INTO outbox_events (id, aggregate_id, event_type, payload, created_at)
SELECT gen_random_uuid(), c.id, 'OfferPublished',
       jsonb_build_object(
           'eventId', NULL, 'eventType', 'OfferPublished', 'version', 1,
           'occurredAt', to_char(c.start_date::timestamp AT TIME ZONE 'America/Lima' AT TIME ZONE 'UTC',
                                 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
           'aggregateId', c.id,
           'payload', jsonb_build_object(
               'offerId', o.id, 'campaignId', c.id, 'businessId', c.business_id,
               'businessName', m.business_name, 'title', o.title, 'category', o.category,
               'price', o.price, 'validTo', to_char(o.valid_to, 'YYYY-MM-DD'),
               'location', CASE WHEN o.location IS NULL THEN NULL ELSE jsonb_build_object(
                   'latitude', ST_Y(o.location::geometry), 'longitude', ST_X(o.location::geometry)) END,
               'zone', jsonb_build_object(
                   'type', c.zone_type,
                   'center', CASE WHEN c.zone_center IS NULL THEN NULL ELSE jsonb_build_object(
                       'latitude', ST_Y(c.zone_center::geometry), 'longitude', ST_X(c.zone_center::geometry)) END,
                   'radiusMeters', c.zone_radius_m, 'district', c.zone_district,
                   'reachMeters', CASE WHEN c.zone_center IS NULL THEN NULL
                                       WHEN c.zone_type = 'DISTRICT' THEN 2000
                                       ELSE c.zone_radius_m END),
               'reactivated', false,
               'publishedAt', to_char(c.start_date::timestamp AT TIME ZONE 'America/Lima' AT TIME ZONE 'UTC',
                                      'YYYY-MM-DD"T"HH24:MI:SS"Z"'))),
       now()
FROM offers o
JOIN campaigns c ON c.id = o.campaign_id
JOIN merchant_standings m ON m.business_id = c.business_id
WHERE o.status = 'PUBLISHED'
  AND c.status = 'ACTIVE'
  AND o.valid_to >= (now() AT TIME ZONE 'America/Lima')::date
ORDER BY c.id, o.id;

UPDATE outbox_events SET payload = jsonb_set(payload, '{eventId}', to_jsonb(id::text))
WHERE event_type = 'OfferPublished' AND payload->>'eventId' IS NULL;
