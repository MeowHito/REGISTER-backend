-- ---------------------------------------------------------------------------
-- Seed data for the `countryState` table (used by the event-creation province
-- dropdown via GET /public-api/master/countryState and by the public
-- registration form's billing province).
--
-- The dropdown showed "ไม่มีข้อมูล" because:
--   1. the query referenced a non-existent `active` column (fixed in
--      MasterDataRepository.getAllCountryState), and
--   2. the table itself had no rows (province master data was never seeded —
--      `geoProvinces` is empty too, so it cannot be copied from there).
--
-- The backend now seeds this automatically on startup if the table is empty
-- (see DataLoader.seedCountryState). This script is the manual equivalent, for
-- when you want to populate the table without redeploying.
--
-- NON-DESTRUCTIVE and idempotent: `INSERT IGNORE` skips rows whose primary key
-- already exists, so re-running will not duplicate or wipe existing data.
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO `countryState` (`id`, `uuid`, `countryEn`, `countryLocal`, `stateEn`, `stateLocal`, `stateType`) VALUES
(1, UUID(), 'Thailand', 'ประเทศไทย', 'Bangkok', 'กรุงเทพมหานคร', 'Province'),
(2, UUID(), 'Thailand', 'ประเทศไทย', 'Krabi', 'กระบี่', 'Province'),
(3, UUID(), 'Thailand', 'ประเทศไทย', 'Kanchanaburi', 'กาญจนบุรี', 'Province'),
(4, UUID(), 'Thailand', 'ประเทศไทย', 'Kalasin', 'กาฬสินธุ์', 'Province'),
(5, UUID(), 'Thailand', 'ประเทศไทย', 'Kamphaeng Phet', 'กำแพงเพชร', 'Province'),
(6, UUID(), 'Thailand', 'ประเทศไทย', 'Khon Kaen', 'ขอนแก่น', 'Province'),
(7, UUID(), 'Thailand', 'ประเทศไทย', 'Chanthaburi', 'จันทบุรี', 'Province'),
(8, UUID(), 'Thailand', 'ประเทศไทย', 'Chachoengsao', 'ฉะเชิงเทรา', 'Province'),
(9, UUID(), 'Thailand', 'ประเทศไทย', 'Chon Buri', 'ชลบุรี', 'Province'),
(10, UUID(), 'Thailand', 'ประเทศไทย', 'Chai Nat', 'ชัยนาท', 'Province'),
(11, UUID(), 'Thailand', 'ประเทศไทย', 'Chaiyaphum', 'ชัยภูมิ', 'Province'),
(12, UUID(), 'Thailand', 'ประเทศไทย', 'Chumphon', 'ชุมพร', 'Province'),
(13, UUID(), 'Thailand', 'ประเทศไทย', 'Chiang Rai', 'เชียงราย', 'Province'),
(14, UUID(), 'Thailand', 'ประเทศไทย', 'Chiang Mai', 'เชียงใหม่', 'Province'),
(15, UUID(), 'Thailand', 'ประเทศไทย', 'Trang', 'ตรัง', 'Province'),
(16, UUID(), 'Thailand', 'ประเทศไทย', 'Trat', 'ตราด', 'Province'),
(17, UUID(), 'Thailand', 'ประเทศไทย', 'Tak', 'ตาก', 'Province'),
(18, UUID(), 'Thailand', 'ประเทศไทย', 'Nakhon Nayok', 'นครนายก', 'Province'),
(19, UUID(), 'Thailand', 'ประเทศไทย', 'Nakhon Pathom', 'นครปฐม', 'Province'),
(20, UUID(), 'Thailand', 'ประเทศไทย', 'Nakhon Phanom', 'นครพนม', 'Province'),
(21, UUID(), 'Thailand', 'ประเทศไทย', 'Nakhon Ratchasima', 'นครราชสีมา', 'Province'),
(22, UUID(), 'Thailand', 'ประเทศไทย', 'Nakhon Si Thammarat', 'นครศรีธรรมราช', 'Province'),
(23, UUID(), 'Thailand', 'ประเทศไทย', 'Nakhon Sawan', 'นครสวรรค์', 'Province'),
(24, UUID(), 'Thailand', 'ประเทศไทย', 'Nonthaburi', 'นนทบุรี', 'Province'),
(25, UUID(), 'Thailand', 'ประเทศไทย', 'Narathiwat', 'นราธิวาส', 'Province'),
(26, UUID(), 'Thailand', 'ประเทศไทย', 'Nan', 'น่าน', 'Province'),
(27, UUID(), 'Thailand', 'ประเทศไทย', 'Bueng Kan', 'บึงกาฬ', 'Province'),
(28, UUID(), 'Thailand', 'ประเทศไทย', 'Buri Ram', 'บุรีรัมย์', 'Province'),
(29, UUID(), 'Thailand', 'ประเทศไทย', 'Pathum Thani', 'ปทุมธานี', 'Province'),
(30, UUID(), 'Thailand', 'ประเทศไทย', 'Prachuap Khiri Khan', 'ประจวบคีรีขันธ์', 'Province'),
(31, UUID(), 'Thailand', 'ประเทศไทย', 'Prachin Buri', 'ปราจีนบุรี', 'Province'),
(32, UUID(), 'Thailand', 'ประเทศไทย', 'Pattani', 'ปัตตานี', 'Province'),
(33, UUID(), 'Thailand', 'ประเทศไทย', 'Phra Nakhon Si Ayutthaya', 'พระนครศรีอยุธยา', 'Province'),
(34, UUID(), 'Thailand', 'ประเทศไทย', 'Phayao', 'พะเยา', 'Province'),
(35, UUID(), 'Thailand', 'ประเทศไทย', 'Phang Nga', 'พังงา', 'Province'),
(36, UUID(), 'Thailand', 'ประเทศไทย', 'Phatthalung', 'พัทลุง', 'Province'),
(37, UUID(), 'Thailand', 'ประเทศไทย', 'Phichit', 'พิจิตร', 'Province'),
(38, UUID(), 'Thailand', 'ประเทศไทย', 'Phitsanulok', 'พิษณุโลก', 'Province'),
(39, UUID(), 'Thailand', 'ประเทศไทย', 'Phetchaburi', 'เพชรบุรี', 'Province'),
(40, UUID(), 'Thailand', 'ประเทศไทย', 'Phetchabun', 'เพชรบูรณ์', 'Province'),
(41, UUID(), 'Thailand', 'ประเทศไทย', 'Phrae', 'แพร่', 'Province'),
(42, UUID(), 'Thailand', 'ประเทศไทย', 'Phuket', 'ภูเก็ต', 'Province'),
(43, UUID(), 'Thailand', 'ประเทศไทย', 'Maha Sarakham', 'มหาสารคาม', 'Province'),
(44, UUID(), 'Thailand', 'ประเทศไทย', 'Mukdahan', 'มุกดาหาร', 'Province'),
(45, UUID(), 'Thailand', 'ประเทศไทย', 'Mae Hong Son', 'แม่ฮ่องสอน', 'Province'),
(46, UUID(), 'Thailand', 'ประเทศไทย', 'Yasothon', 'ยโสธร', 'Province'),
(47, UUID(), 'Thailand', 'ประเทศไทย', 'Yala', 'ยะลา', 'Province'),
(48, UUID(), 'Thailand', 'ประเทศไทย', 'Roi Et', 'ร้อยเอ็ด', 'Province'),
(49, UUID(), 'Thailand', 'ประเทศไทย', 'Ranong', 'ระนอง', 'Province'),
(50, UUID(), 'Thailand', 'ประเทศไทย', 'Rayong', 'ระยอง', 'Province'),
(51, UUID(), 'Thailand', 'ประเทศไทย', 'Ratchaburi', 'ราชบุรี', 'Province'),
(52, UUID(), 'Thailand', 'ประเทศไทย', 'Lop Buri', 'ลพบุรี', 'Province'),
(53, UUID(), 'Thailand', 'ประเทศไทย', 'Lampang', 'ลำปาง', 'Province'),
(54, UUID(), 'Thailand', 'ประเทศไทย', 'Lamphun', 'ลำพูน', 'Province'),
(55, UUID(), 'Thailand', 'ประเทศไทย', 'Loei', 'เลย', 'Province'),
(56, UUID(), 'Thailand', 'ประเทศไทย', 'Si Sa Ket', 'ศรีสะเกษ', 'Province'),
(57, UUID(), 'Thailand', 'ประเทศไทย', 'Sakon Nakhon', 'สกลนคร', 'Province'),
(58, UUID(), 'Thailand', 'ประเทศไทย', 'Songkhla', 'สงขลา', 'Province'),
(59, UUID(), 'Thailand', 'ประเทศไทย', 'Satun', 'สตูล', 'Province'),
(60, UUID(), 'Thailand', 'ประเทศไทย', 'Samut Prakan', 'สมุทรปราการ', 'Province'),
(61, UUID(), 'Thailand', 'ประเทศไทย', 'Samut Songkhram', 'สมุทรสงคราม', 'Province'),
(62, UUID(), 'Thailand', 'ประเทศไทย', 'Samut Sakhon', 'สมุทรสาคร', 'Province'),
(63, UUID(), 'Thailand', 'ประเทศไทย', 'Sa Kaeo', 'สระแก้ว', 'Province'),
(64, UUID(), 'Thailand', 'ประเทศไทย', 'Saraburi', 'สระบุรี', 'Province'),
(65, UUID(), 'Thailand', 'ประเทศไทย', 'Sing Buri', 'สิงห์บุรี', 'Province'),
(66, UUID(), 'Thailand', 'ประเทศไทย', 'Sukhothai', 'สุโขทัย', 'Province'),
(67, UUID(), 'Thailand', 'ประเทศไทย', 'Suphan Buri', 'สุพรรณบุรี', 'Province'),
(68, UUID(), 'Thailand', 'ประเทศไทย', 'Surat Thani', 'สุราษฎร์ธานี', 'Province'),
(69, UUID(), 'Thailand', 'ประเทศไทย', 'Surin', 'สุรินทร์', 'Province'),
(70, UUID(), 'Thailand', 'ประเทศไทย', 'Nong Khai', 'หนองคาย', 'Province'),
(71, UUID(), 'Thailand', 'ประเทศไทย', 'Nong Bua Lam Phu', 'หนองบัวลำภู', 'Province'),
(72, UUID(), 'Thailand', 'ประเทศไทย', 'Ang Thong', 'อ่างทอง', 'Province'),
(73, UUID(), 'Thailand', 'ประเทศไทย', 'Amnat Charoen', 'อำนาจเจริญ', 'Province'),
(74, UUID(), 'Thailand', 'ประเทศไทย', 'Udon Thani', 'อุดรธานี', 'Province'),
(75, UUID(), 'Thailand', 'ประเทศไทย', 'Uttaradit', 'อุตรดิตถ์', 'Province'),
(76, UUID(), 'Thailand', 'ประเทศไทย', 'Uthai Thani', 'อุทัยธานี', 'Province'),
(77, UUID(), 'Thailand', 'ประเทศไทย', 'Ubon Ratchathani', 'อุบลราชธานี', 'Province');
