CREATE TYPE "public"."notification_kind" AS ENUM('new_review', 'reply_rejected', 'auth_revoked');--> statement-breakpoint
CREATE TABLE "line_bind_codes" (
	"code" text PRIMARY KEY NOT NULL,
	"organization_id" uuid NOT NULL,
	"expires_at" timestamp with time zone NOT NULL,
	"used_at" timestamp with time zone,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "line_events" (
	"webhook_event_id" text PRIMARY KEY NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "line_messages" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid NOT NULL,
	"binding_id" uuid NOT NULL,
	"location_id" uuid,
	"kind" "notification_kind" NOT NULL,
	"items" jsonb DEFAULT '[]'::jsonb NOT NULL,
	"status" text NOT NULL,
	"error" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "notifications" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"organization_id" uuid NOT NULL,
	"location_id" uuid,
	"review_id" uuid,
	"kind" "notification_kind" NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"sent_at" timestamp with time zone
);
--> statement-breakpoint
ALTER TABLE "line_bindings" ADD COLUMN "following" boolean DEFAULT true NOT NULL;--> statement-breakpoint
ALTER TABLE "line_bindings" ADD COLUMN "last_review_id" uuid;--> statement-breakpoint
ALTER TABLE "replies" ADD COLUMN "verified_at" timestamp with time zone;--> statement-breakpoint
ALTER TABLE "line_bind_codes" ADD CONSTRAINT "line_bind_codes_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "line_messages" ADD CONSTRAINT "line_messages_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "line_messages" ADD CONSTRAINT "line_messages_binding_id_line_bindings_id_fk" FOREIGN KEY ("binding_id") REFERENCES "public"."line_bindings"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "line_messages" ADD CONSTRAINT "line_messages_location_id_locations_id_fk" FOREIGN KEY ("location_id") REFERENCES "public"."locations"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_location_id_locations_id_fk" FOREIGN KEY ("location_id") REFERENCES "public"."locations"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_review_id_reviews_id_fk" FOREIGN KEY ("review_id") REFERENCES "public"."reviews"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "line_messages_location_idx" ON "line_messages" USING btree ("location_id","kind","created_at");--> statement-breakpoint
CREATE INDEX "notifications_pending_idx" ON "notifications" USING btree ("location_id","sent_at");--> statement-breakpoint
ALTER TABLE "line_bindings" ADD CONSTRAINT "line_bindings_last_review_id_reviews_id_fk" FOREIGN KEY ("last_review_id") REFERENCES "public"."reviews"("id") ON DELETE set null ON UPDATE no action;