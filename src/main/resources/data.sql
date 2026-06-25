
INSERT INTO membership_plans (id, name, billing_period, base_price, currency, is_active) VALUES
('plan_monthly_std', 'Standard Monthly Club', 'MONTHLY', 199.00, 'INR', true),
('plan_quarterly_adv', 'Advantage Quarterly', 'QUARTERLY', 499.00, 'INR', true),
('plan_yearly_plat', 'Elite Annual Pass', 'YEARLY', 1499.00, 'INR', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO membership_tiers (id, tier_level, name, min_orders_required, min_order_value_monthly, target_cohort_id) VALUES
('tier_silver', 1, 'Silver Elite', 5, 2000.00, NULL),
('tier_gold', 2, 'Gold Premium', 15, 7500.00, NULL),
('tier_platinum', 3, 'Platinum VIP', 30, 20000.00, 'VIP_POWER_USERS')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tier_benefits (id, tier_id, benefit_type, configuration_json, is_configurable) VALUES
('b_silver_deliv', 'tier_silver', 'FREE_DELIVERY', '{"min_cart_value": 499}', true),
('b_gold_deliv', 'tier_gold', 'FREE_DELIVERY', '{"min_cart_value": 0}', true),
('b_gold_disc', 'tier_gold', 'CATEGORY_DISCOUNT', '{"discount_pct": 5, "categories": ["Apparel"]}', true),
('b_plat_disc', 'tier_platinum', 'CATEGORY_DISCOUNT', '{"discount_pct": 15, "categories": ["All"]}', true),
('b_plat_support', 'tier_platinum', 'PRIORITY_SUPPORT', '{"channel": "IMMEDIATE_CALL_BACK"}', true)
ON CONFLICT (id) DO NOTHING;