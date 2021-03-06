ALTER TABLE sec_cash ADD COLUMN rate DOUBLE PRECISION NOT NULL;
ALTER TABLE sec_cash ADD COLUMN amount DOUBLE PRECISION NOT NULL;
ALTER TABLE sec_fra ADD COLUMN rate DOUBLE PRECISION NOT NULL;
ALTER TABLE sec_fra ADD COLUMN amount DOUBLE PRECISION NOT NULL;

ALTER TABLE sec_future ADD COLUMN bondFutureFirstDeliveryDate TIMESTAMP;
ALTER TABLE sec_future ADD COLUMN bondFutureFirstDeliveryDate_zone VARCHAR(50);
ALTER TABLE sec_future ADD COLUMN bondFutureLastDeliveryDate TIMESTAMP;
ALTER TABLE sec_future ADD COLUMN bondFutureLastDeliveryDate_zone VARCHAR(50);

ALTER TABLE pos_trade
 ADD COLUMN provider_scheme varchar(255);
ALTER TABLE pos_trade
 ADD COLUMN provider_value varchar(255);
