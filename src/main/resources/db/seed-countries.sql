-- ---------------------------------------------------------------------------
-- Master `countries` table used by GET /public-api/master/nationalities
-- (MasterDataRepository.findAllNationalities → SELECT alpha_3_code, nationality FROM countries).
--
-- This table is NOT backed by a JPA entity, so `spring.jpa.hibernate.ddl-auto: create`
-- never creates it and the endpoint returns HTTP 500. Run this once against the
-- database; because Hibernate only manages entity-mapped tables, the rows persist
-- across app restarts.
--
--   mysql -h 127.0.0.1 -P 3307 -uroot -plocaldev membership_db < seed-countries.sql
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `countries` (
  `alpha_3_code` VARCHAR(3) NOT NULL,
  `nationality`  VARCHAR(255) NOT NULL,
  PRIMARY KEY (`alpha_3_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `countries` (`alpha_3_code`, `nationality`) VALUES
('THA','Thai'),
('AFG','Afghan'),('ALB','Albanian'),('DZA','Algerian'),('AND','Andorran'),
('AGO','Angolan'),('ATG','Antiguan'),('ARG','Argentine'),('ARM','Armenian'),
('AUS','Australian'),('AUT','Austrian'),('AZE','Azerbaijani'),('BHS','Bahamian'),
('BHR','Bahraini'),('BGD','Bangladeshi'),('BRB','Barbadian'),('BLR','Belarusian'),
('BEL','Belgian'),('BLZ','Belizean'),('BEN','Beninese'),('BTN','Bhutanese'),
('BOL','Bolivian'),('BIH','Bosnian'),('BWA','Botswanan'),('BRA','Brazilian'),
('BRN','Bruneian'),('BGR','Bulgarian'),('BFA','Burkinabe'),('BDI','Burundian'),
('KHM','Cambodian'),('CMR','Cameroonian'),('CAN','Canadian'),('CPV','Cape Verdean'),
('CAF','Central African'),('TCD','Chadian'),('CHL','Chilean'),('CHN','Chinese'),
('COL','Colombian'),('COM','Comoran'),('COG','Congolese'),('COD','Congolese (DRC)'),
('CRI','Costa Rican'),('CIV','Ivorian'),('HRV','Croatian'),('CUB','Cuban'),
('CYP','Cypriot'),('CZE','Czech'),('DNK','Danish'),('DJI','Djiboutian'),
('DMA','Dominican'),('DOM','Dominican (Republic)'),('ECU','Ecuadorian'),('EGY','Egyptian'),
('SLV','Salvadoran'),('GNQ','Equatorial Guinean'),('ERI','Eritrean'),('EST','Estonian'),
('SWZ','Swazi'),('ETH','Ethiopian'),('FJI','Fijian'),('FIN','Finnish'),
('FRA','French'),('GAB','Gabonese'),('GMB','Gambian'),('GEO','Georgian'),
('DEU','German'),('GHA','Ghanaian'),('GRC','Greek'),('GRD','Grenadian'),
('GTM','Guatemalan'),('GIN','Guinean'),('GNB','Bissau-Guinean'),('GUY','Guyanese'),
('HTI','Haitian'),('HND','Honduran'),('HUN','Hungarian'),('ISL','Icelandic'),
('IND','Indian'),('IDN','Indonesian'),('IRN','Iranian'),('IRQ','Iraqi'),
('IRL','Irish'),('ISR','Israeli'),('ITA','Italian'),('JAM','Jamaican'),
('JPN','Japanese'),('JOR','Jordanian'),('KAZ','Kazakhstani'),('KEN','Kenyan'),
('KIR','I-Kiribati'),('PRK','North Korean'),('KOR','South Korean'),('KWT','Kuwaiti'),
('KGZ','Kyrgyz'),('LAO','Lao'),('LVA','Latvian'),('LBN','Lebanese'),
('LSO','Basotho'),('LBR','Liberian'),('LBY','Libyan'),('LIE','Liechtensteiner'),
('LTU','Lithuanian'),('LUX','Luxembourger'),('MDG','Malagasy'),('MWI','Malawian'),
('MYS','Malaysian'),('MDV','Maldivian'),('MLI','Malian'),('MLT','Maltese'),
('MHL','Marshallese'),('MRT','Mauritanian'),('MUS','Mauritian'),('MEX','Mexican'),
('FSM','Micronesian'),('MDA','Moldovan'),('MCO','Monegasque'),('MNG','Mongolian'),
('MNE','Montenegrin'),('MAR','Moroccan'),('MOZ','Mozambican'),('MMR','Burmese'),
('NAM','Namibian'),('NRU','Nauruan'),('NPL','Nepali'),('NLD','Dutch'),
('NZL','New Zealander'),('NIC','Nicaraguan'),('NER','Nigerien'),('NGA','Nigerian'),
('MKD','Macedonian'),('NOR','Norwegian'),('OMN','Omani'),('PAK','Pakistani'),
('PLW','Palauan'),('PSE','Palestinian'),('PAN','Panamanian'),('PNG','Papua New Guinean'),
('PRY','Paraguayan'),('PER','Peruvian'),('PHL','Filipino'),('POL','Polish'),
('PRT','Portuguese'),('QAT','Qatari'),('ROU','Romanian'),('RUS','Russian'),
('RWA','Rwandan'),('KNA','Kittitian'),('LCA','Saint Lucian'),('VCT','Vincentian'),
('WSM','Samoan'),('SMR','Sammarinese'),('STP','Sao Tomean'),('SAU','Saudi'),
('SEN','Senegalese'),('SRB','Serbian'),('SYC','Seychellois'),('SLE','Sierra Leonean'),
('SGP','Singaporean'),('SVK','Slovak'),('SVN','Slovenian'),('SLB','Solomon Islander'),
('SOM','Somali'),('ZAF','South African'),('SSD','South Sudanese'),('ESP','Spanish'),
('LKA','Sri Lankan'),('SDN','Sudanese'),('SUR','Surinamese'),('SWE','Swedish'),
('CHE','Swiss'),('SYR','Syrian'),('TWN','Taiwanese'),('TJK','Tajik'),
('TZA','Tanzanian'),('TLS','Timorese'),('TGO','Togolese'),('TON','Tongan'),
('TTO','Trinidadian'),('TUN','Tunisian'),('TUR','Turkish'),('TKM','Turkmen'),
('TUV','Tuvaluan'),('UGA','Ugandan'),('UKR','Ukrainian'),('ARE','Emirati'),
('GBR','British'),('USA','American'),('URY','Uruguayan'),('UZB','Uzbek'),
('VUT','Ni-Vanuatu'),('VAT','Vatican'),('VEN','Venezuelan'),('VNM','Vietnamese'),
('YEM','Yemeni'),('ZMB','Zambian'),('ZWE','Zimbabwean')
ON DUPLICATE KEY UPDATE `nationality` = VALUES(`nationality`);
