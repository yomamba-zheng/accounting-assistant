-- 插入默认用户 (密码: 123456)
INSERT INTO t_user (username, password, email, status) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'test@example.com', 1);

-- 插入支出分类
INSERT INTO t_category (name, icon, color, type, parent_id, sort_order) VALUES
('餐饮', '🍔', '#FF6B6B', 1, NULL, 1),
('交通', '🚗', '#4ECDC4', 1, NULL, 2),
('购物', '🛍️', '#45B7D1', 1, NULL, 3),
('娱乐', '🎮', '#96CEB4', 1, NULL, 4),
('居住', '🏠', '#FFEAA7', 1, NULL, 5),
('医疗', '💊', '#DDA0DD', 1, NULL, 6),
('教育', '📚', '#98D8C8', 1, NULL, 7),
('其他', '📝', '#C0C0C0', 1, NULL, 8);

-- 插入收入分类
INSERT INTO t_category (name, icon, color, type, parent_id, sort_order) VALUES
('工资', '💰', '#2ECC71', 2, NULL, 1),
('奖金', '🎁', '#27AE60', 2, NULL, 2),
('投资', '📈', '#3498DB', 2, NULL, 3),
('其他收入', '💵', '#95A5A6', 2, NULL, 4);

-- 插入餐饮子分类
INSERT INTO t_category (name, icon, color, type, parent_id, sort_order) VALUES
('早餐', '🥐', '#FFB6C1', 1, 1, 1),
('午餐', '🍱', '#FFA07A', 1, 1, 2),
('晚餐', '🍜', '#FFD700', 1, 1, 3),
('零食', '🍪', '#DDA0DD', 1, 1, 4),
('外卖', '🍔', '#FF6347', 1, 1, 5);

-- 插入交通子分类
INSERT INTO t_category (name, icon, color, type, parent_id, sort_order) VALUES
('公交', '🚌', '#87CEEB', 1, 2, 1),
('地铁', '🚇', '#4682B4', 1, 2, 2),
('打车', '🚕', '#FFA500', 1, 2, 3),
('加油', '⛽', '#DC143C', 1, 2, 4);

-- 插入分类规则（关键词匹配）- 现在匹配到小类ID
-- 餐饮小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
-- 早餐 (category_id=9)
(9, '早餐', 2), (9, '早点', 2), (9, '包子', 2), (9, '豆浆', 2), (9, '油条', 2), (9, '牛奶', 2),
-- 午餐 (category_id=10)
(10, '午餐', 2), (10, '午饭', 2), (10, '中饭', 2),
-- 晚餐 (category_id=11)
(11, '晚餐', 2), (11, '晚饭', 2), (11, '夜宵', 2),
-- 外卖 (category_id=13)
(13, '外卖', 2), (13, '快餐', 2), (13, '饭店', 2), (13, '餐厅', 2), (13, '点餐', 2),
-- 零食 (category_id=12)
(12, '零食', 2), (12, '小吃', 2), (12, '奶茶', 2), (12, '水果', 2), (12, '饮料', 2);

-- 交通小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
-- 公交 (category_id=14)
(14, '公交', 2), (14, '公交车', 2), (14, '巴士', 2),
-- 地铁 (category_id=15)
(15, '地铁', 2), (15, '地铁票', 2),
-- 打车 (category_id=16)
(16, '出租', 2), (16, '打车', 2), (16, '滴滴', 2), (16, '出租车', 2),
-- 加油 (category_id=17)
(17, '加油', 2), (17, '汽油', 2), (17, '油价', 2), (17, '停车', 2);

-- 购物小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
(18, '淘宝', 2), (18, '京东', 2), (18, '拼多多', 2), (18, '网购', 2), (18, '快递', 2),
(19, '衣服', 2), (19, '鞋子', 2), (19, '裤子', 2), (19, '裙子', 2),
(20, '化妆品', 2), (20, '护肤品', 2), (20, '口红', 2), (20, '面膜', 2);

-- 娱乐小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
(21, '电影', 2), (21, '影院', 2),
(22, '网吧', 2), (22, '游戏', 2), (22, '电竞', 2),
(23, '唱歌', 2), (23, 'KTV', 2), (23, '酒吧', 2),
(24, '旅游', 2), (24, '度假', 2), (24, '门票', 2),
(25, '健身', 2), (25, '运动', 2), (25, '游泳', 2), (25, '瑜伽', 2);

-- 居住小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
(26, '房租', 2), (26, '租金', 2),
(27, '水电', 2), (27, '电费', 2), (27, '水费', 2), (27, '燃气', 2), (27, '物业', 2);

-- 医疗小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
(28, '药', 2), (28, '药店', 2),
(29, '医院', 2), (29, '诊所', 2), (29, '看病', 2), (29, '门诊', 2);

-- 教育小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
(30, '书', 2), (30, '图书', 2), (30, '教材', 2),
(31, '培训', 2), (31, '课程', 2), (31, '学费', 2), (31, '辅导', 2);

-- 收入小类规则
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
(33, '工资', 2), (33, '月薪', 2), (33, '薪资', 2), (33, '底薪', 2),
(34, '奖金', 2), (34, '年终奖', 2), (34, '提成', 2), (34, '分红', 2),
(35, '投资', 2), (35, '理财', 2), (35, '收益', 2), (35, '利息', 2);

-- 兜底规则 - 匹配到大类（优先级较低）
INSERT INTO t_category_rule (category_id, keyword, priority) VALUES
(1, '吃饭', 1), (1, '餐饮', 1), (1, '饭', 1), (1, '吃', 1),
(2, '交通', 1),
(3, '购物', 1), (3, '网购', 1),
(4, '娱乐', 1), (4, '玩', 1),
(5, '居住', 1), (5, '住房', 1),
(6, '医疗', 1), (6, '看病', 1),
(7, '教育', 1), (7, '学习', 1);

-- 时间上下文规则（11:00-13:00 + 饭/吃 = 午餐）
INSERT INTO t_category_rule (category_id, keyword, time_start, time_end, priority) VALUES
(10, '饭', '11:00:00', '13:00:00', 1),
(10, '吃', '11:00:00', '13:00:00', 1);

-- 时间上下文规则（18:00-20:00 + 吃/饭 = 晚餐）
INSERT INTO t_category_rule (category_id, keyword, time_start, time_end, priority) VALUES
(11, '吃', '18:00:00', '20:00:00', 1),
(11, '饭', '18:00:00', '20:00:00', 1);
