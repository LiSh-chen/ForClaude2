CREATE TYPE "public"."ai_status" AS ENUM('none', 'done', 'failed', 'quota_exceeded');--> statement-breakpoint
CREATE TYPE "public"."credential_status" AS ENUM('active', 'revoked', 'expired');--> statement-breakpoint
CREATE TYPE "public"."industry" AS ENUM('food', 'beauty', 'medical', 'education', 'other');--> statement-breakpoint
CREATE TYPE "public"."review_language" AS ENUM('zh-TW', 'en', 'ja', 'other');--> statement-breakpoint
CREATE TYPE "public"."publish_status" AS ENUM('pending', 'published', 'failed', 'rejected');--> statement-breakpoint
CREATE TYPE "public"."published_by" AS ENUM('user', 'line_user', 'auto', 'external');--> statement-breakpoint
CREATE TYPE "public"."review_status" AS ENUM('pending', 'replied', 'skipped', 'publish_failed');--> statement-breakpoint
CREATE TYPE "public"."sentiment" AS ENUM('positive', 'neutral', 'negative');--> statement-breakpoint
CREATE TYPE "public"."subscription_status" AS ENUM('trialing', 'active', 'past_due', 'canceled', 'expired');--> statement-breakpoint
CREATE TABLE "ai_usage_logs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid,
	"purpose" text NOT NULL,
	"model" text NOT NULL,
	"tokens_in" integer DEFAULT 0 NOT NULL,
	"tokens_out" integer DEFAULT 0 NOT NULL,
	"cost" numeric(12, 6) DEFAULT '0' NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "brand_settings" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid NOT NULL,
	"tone" text DEFAULT '親切' NOT NULL,
	"salutation" text DEFAULT '我們' NOT NULL,
	"signature" text,
	"store_info" text,
	"auto_reply_enabled" boolean DEFAULT false NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "brand_settings_organization_id_unique" UNIQUE("organization_id")
);
--> statement-breakpoint
CREATE TABLE "geo_checks" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"store_name" text NOT NULL,
	"area" text NOT NULL,
	"industry" text NOT NULL,
	"results" jsonb,
	"cost" numeric(12, 6) DEFAULT '0' NOT NULL,
	"ip_hash" text NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "google_credentials" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"refresh_token_encrypted" text NOT NULL,
	"scopes" text,
	"status" "credential_status" DEFAULT 'active' NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "google_credentials_user_id_unique" UNIQUE("user_id")
);
--> statement-breakpoint
CREATE TABLE "invoices" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid NOT NULL,
	"subscription_id" uuid,
	"ecpay_trade_no" text,
	"invoice_number" text,
	"amount_twd" integer NOT NULL,
	"tax_id" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "invoices_ecpay_trade_no_unique" UNIQUE("ecpay_trade_no")
);
--> statement-breakpoint
CREATE TABLE "leads" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"email" text,
	"line_user_id" text,
	"source" text NOT NULL,
	"geo_check_id" uuid,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "line_binding_locations" (
	"line_binding_id" uuid NOT NULL,
	"location_id" uuid NOT NULL,
	CONSTRAINT "line_binding_locations_line_binding_id_location_id_pk" PRIMARY KEY("line_binding_id","location_id")
);
--> statement-breakpoint
CREATE TABLE "line_bindings" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid NOT NULL,
	"line_user_id" text NOT NULL,
	"display_name" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "locations" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid NOT NULL,
	"google_location_id" text NOT NULL,
	"name" text NOT NULL,
	"industry" "industry" DEFAULT 'other' NOT NULL,
	"enabled" boolean DEFAULT false NOT NULL,
	"initial_import_done" boolean DEFAULT false NOT NULL,
	"last_synced_at" timestamp with time zone,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "monthly_reports" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"location_id" uuid NOT NULL,
	"month" text NOT NULL,
	"data" jsonb NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "organizations" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"name" text NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "replies" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"review_id" uuid NOT NULL,
	"content" text NOT NULL,
	"published_at" timestamp with time zone,
	"publish_status" "publish_status" DEFAULT 'pending' NOT NULL,
	"published_by" "published_by" NOT NULL,
	"published_by_line_user_id" text,
	"error_message" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "replies_review_id_unique" UNIQUE("review_id")
);
--> statement-breakpoint
CREATE TABLE "reply_drafts" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"review_id" uuid NOT NULL,
	"content" text NOT NULL,
	"version" integer DEFAULT 1 NOT NULL,
	"model" text NOT NULL,
	"tokens_in" integer DEFAULT 0 NOT NULL,
	"tokens_out" integer DEFAULT 0 NOT NULL,
	"cost" numeric(12, 6) DEFAULT '0' NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "review_analyses" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"review_id" uuid NOT NULL,
	"sentiment" "sentiment" NOT NULL,
	"topics" jsonb DEFAULT '[]'::jsonb NOT NULL,
	"language" "review_language" NOT NULL,
	"needs_human" boolean DEFAULT false NOT NULL,
	"risk_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "review_analyses_review_id_unique" UNIQUE("review_id")
);
--> statement-breakpoint
CREATE TABLE "reviews" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"google_review_id" text NOT NULL,
	"location_id" uuid NOT NULL,
	"star_rating" integer NOT NULL,
	"comment" text,
	"reviewer_display_name" text,
	"create_time" timestamp with time zone NOT NULL,
	"update_time" timestamp with time zone NOT NULL,
	"is_edited" boolean DEFAULT false NOT NULL,
	"is_historical" boolean DEFAULT false NOT NULL,
	"status" "review_status" DEFAULT 'pending' NOT NULL,
	"ai_status" "ai_status" DEFAULT 'none' NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "reviews_star_rating_range" CHECK ("reviews"."star_rating" between 1 and 5)
);
--> statement-breakpoint
CREATE TABLE "subscriptions" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid NOT NULL,
	"plan" text DEFAULT 'trial' NOT NULL,
	"status" "subscription_status" DEFAULT 'trialing' NOT NULL,
	"trial_ends_at" timestamp with time zone,
	"current_period_end" timestamp with time zone,
	"cancel_at_period_end" boolean DEFAULT false NOT NULL,
	"pending_plan" text,
	"ecpay_merchant_trade_no" text,
	"ecpay_gwsr" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "subscriptions_organization_id_unique" UNIQUE("organization_id")
);
--> statement-breakpoint
CREATE TABLE "usage_counters" (
	"organization_id" uuid NOT NULL,
	"month" text NOT NULL,
	"drafts_used" integer DEFAULT 0 NOT NULL,
	"drafts_bonus" integer DEFAULT 0 NOT NULL,
	CONSTRAINT "usage_counters_organization_id_month_pk" PRIMARY KEY("organization_id","month")
);
--> statement-breakpoint
CREATE TABLE "users" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid,
	"google_sub" text NOT NULL,
	"email" text NOT NULL,
	"name" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "users_google_sub_unique" UNIQUE("google_sub")
);
--> statement-breakpoint
ALTER TABLE "ai_usage_logs" ADD CONSTRAINT "ai_usage_logs_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "brand_settings" ADD CONSTRAINT "brand_settings_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "google_credentials" ADD CONSTRAINT "google_credentials_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "invoices" ADD CONSTRAINT "invoices_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "invoices" ADD CONSTRAINT "invoices_subscription_id_subscriptions_id_fk" FOREIGN KEY ("subscription_id") REFERENCES "public"."subscriptions"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "leads" ADD CONSTRAINT "leads_geo_check_id_geo_checks_id_fk" FOREIGN KEY ("geo_check_id") REFERENCES "public"."geo_checks"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "line_binding_locations" ADD CONSTRAINT "line_binding_locations_line_binding_id_line_bindings_id_fk" FOREIGN KEY ("line_binding_id") REFERENCES "public"."line_bindings"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "line_binding_locations" ADD CONSTRAINT "line_binding_locations_location_id_locations_id_fk" FOREIGN KEY ("location_id") REFERENCES "public"."locations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "line_bindings" ADD CONSTRAINT "line_bindings_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "locations" ADD CONSTRAINT "locations_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "monthly_reports" ADD CONSTRAINT "monthly_reports_location_id_locations_id_fk" FOREIGN KEY ("location_id") REFERENCES "public"."locations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "replies" ADD CONSTRAINT "replies_review_id_reviews_id_fk" FOREIGN KEY ("review_id") REFERENCES "public"."reviews"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "reply_drafts" ADD CONSTRAINT "reply_drafts_review_id_reviews_id_fk" FOREIGN KEY ("review_id") REFERENCES "public"."reviews"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "review_analyses" ADD CONSTRAINT "review_analyses_review_id_reviews_id_fk" FOREIGN KEY ("review_id") REFERENCES "public"."reviews"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "reviews" ADD CONSTRAINT "reviews_location_id_locations_id_fk" FOREIGN KEY ("location_id") REFERENCES "public"."locations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "subscriptions" ADD CONSTRAINT "subscriptions_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "usage_counters" ADD CONSTRAINT "usage_counters_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "users" ADD CONSTRAINT "users_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "ai_usage_logs_org_time_idx" ON "ai_usage_logs" USING btree ("organization_id","created_at");--> statement-breakpoint
CREATE INDEX "geo_checks_lookup_idx" ON "geo_checks" USING btree ("store_name","area","industry");--> statement-breakpoint
CREATE INDEX "geo_checks_ip_idx" ON "geo_checks" USING btree ("ip_hash","created_at");--> statement-breakpoint
CREATE UNIQUE INDEX "line_bindings_org_user_idx" ON "line_bindings" USING btree ("organization_id","line_user_id");--> statement-breakpoint
CREATE UNIQUE INDEX "locations_org_google_idx" ON "locations" USING btree ("organization_id","google_location_id");--> statement-breakpoint
CREATE INDEX "locations_enabled_idx" ON "locations" USING btree ("enabled");--> statement-breakpoint
CREATE UNIQUE INDEX "monthly_reports_location_month_idx" ON "monthly_reports" USING btree ("location_id","month");--> statement-breakpoint
CREATE UNIQUE INDEX "reply_drafts_review_version_idx" ON "reply_drafts" USING btree ("review_id","version");--> statement-breakpoint
CREATE UNIQUE INDEX "reviews_google_review_idx" ON "reviews" USING btree ("location_id","google_review_id");--> statement-breakpoint
CREATE INDEX "reviews_location_status_idx" ON "reviews" USING btree ("location_id","status");