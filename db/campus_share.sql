/*
 Navicat Premium Dump SQL

 Source Server         : oo
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : campus_share

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 16/05/2026 16:29:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for announcement
-- ----------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `pinned` tinyint(1) NULL DEFAULT 0,
  `publish_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of announcement
-- ----------------------------
INSERT INTO `announcement` VALUES (2, '期末周自习室开放安排', '本周公共自习室开放时间调整。', '<p>期末周一至周五自习室开放至晚上 23:00，请同学们合理安排时间。</p>', 1, '2025-12-03 17:20:30', '2025-12-03 17:20:30', '2025-12-03 17:20:30');
INSERT INTO `announcement` VALUES (3, '资料分享规范提醒', '请勿上传侵权或不当内容', '<p>请同学们遵守平台规范，仅上传与学习相关且无版权争议的资料。</p>', 0, '2025-12-03 17:20:30', '2025-12-03 17:20:30', '2025-12-08 09:10:53');
INSERT INTO `announcement` VALUES (4, 'aa', 'sccssdcc', 'acs', 0, '2025-12-10 18:05:19', '2025-12-10 18:05:19', '2025-12-10 18:05:19');

-- ----------------------------
-- Table structure for friend_request
-- ----------------------------
DROP TABLE IF EXISTS `friend_request`;
CREATE TABLE `friend_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_to_user_status`(`to_user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_from_user`(`from_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_fr_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_fr_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of friend_request
-- ----------------------------
INSERT INTO `friend_request` VALUES (1, 2, 3, 'l', 'ACCEPTED', '2025-12-14 15:24:03', '2025-12-14 15:24:37');
INSERT INTO `friend_request` VALUES (2, 2, 4, '', 'PENDING', '2025-12-14 15:42:34', '2025-12-14 15:42:34');
INSERT INTO `friend_request` VALUES (3, 2, 3, '', 'ACCEPTED', '2025-12-14 15:57:45', '2025-12-14 15:58:32');
INSERT INTO `friend_request` VALUES (4, 3, 2, '', 'ACCEPTED', '2025-12-14 16:20:32', '2025-12-14 16:20:45');
INSERT INTO `friend_request` VALUES (5, 3, 2, '', 'ACCEPTED', '2025-12-14 16:34:34', '2025-12-14 16:38:08');
INSERT INTO `friend_request` VALUES (6, 3, 2, '', 'ACCEPTED', '2025-12-14 16:43:18', '2025-12-14 16:43:27');
INSERT INTO `friend_request` VALUES (7, 3, 2, '', 'ACCEPTED', '2025-12-14 16:44:20', '2025-12-14 16:44:29');
INSERT INTO `friend_request` VALUES (8, 2, 3, '', 'ACCEPTED', '2025-12-14 16:50:07', '2025-12-14 16:50:11');
INSERT INTO `friend_request` VALUES (9, 3, 2, '', 'ACCEPTED', '2025-12-14 16:58:00', '2025-12-14 16:58:08');
INSERT INTO `friend_request` VALUES (10, 3, 2, '', 'ACCEPTED', '2025-12-14 17:02:54', '2025-12-14 17:02:57');
INSERT INTO `friend_request` VALUES (11, 3, 2, '', 'ACCEPTED', '2025-12-14 17:06:10', '2025-12-14 17:06:14');
INSERT INTO `friend_request` VALUES (12, 2, 3, '', 'ACCEPTED', '2025-12-14 17:07:04', '2025-12-14 17:07:10');
INSERT INTO `friend_request` VALUES (13, 2, 3, '', 'ACCEPTED', '2025-12-14 17:49:58', '2025-12-14 17:50:11');
INSERT INTO `friend_request` VALUES (14, 1, 3, '111', 'ACCEPTED', '2025-12-15 08:27:30', '2025-12-15 08:27:57');
INSERT INTO `friend_request` VALUES (15, 3, 4, '', 'ACCEPTED', '2025-12-21 19:35:55', '2025-12-21 19:36:26');
INSERT INTO `friend_request` VALUES (16, 4, 1, '', 'ACCEPTED', '2025-12-21 20:56:13', '2025-12-21 20:56:39');
INSERT INTO `friend_request` VALUES (17, 1, 2, '', 'ACCEPTED', '2026-05-15 18:01:29', '2026-05-15 18:02:08');
INSERT INTO `friend_request` VALUES (18, 2, 3, '', 'REJECTED', '2026-05-15 18:17:17', '2026-05-15 18:20:22');
INSERT INTO `friend_request` VALUES (19, 2, 3, '', 'PENDING', '2026-05-15 18:20:44', '2026-05-15 18:20:44');
INSERT INTO `friend_request` VALUES (20, 2, 1, '', 'ACCEPTED', '2026-05-16 15:28:55', '2026-05-16 15:30:00');

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_by_from_user` tinyint(1) NOT NULL DEFAULT 0 COMMENT '发送方是否删除',
  `deleted_by_to_user` tinyint(1) NOT NULL DEFAULT 0 COMMENT '接收方是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_to_user_read`(`to_user_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_from_user`(`from_user_id` ASC) USING BTREE,
  INDEX `idx_conversation`(`from_user_id` ASC, `to_user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_msg_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_msg_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of message
-- ----------------------------
INSERT INTO `message` VALUES (9, 2, 3, '1', 1, '2025-12-14 21:35:09', 1, 0);
INSERT INTO `message` VALUES (10, 3, 2, '1', 1, '2025-12-14 23:42:11', 0, 1);
INSERT INTO `message` VALUES (11, 3, 2, 'ss', 1, '2025-12-15 08:32:09', 0, 1);
INSERT INTO `message` VALUES (12, 2, 3, 'w', 1, '2025-12-15 08:36:56', 0, 0);
INSERT INTO `message` VALUES (13, 3, 1, '1', 1, '2025-12-15 09:05:22', 0, 0);
INSERT INTO `message` VALUES (14, 3, 1, '1', 1, '2025-12-15 09:09:58', 0, 0);
INSERT INTO `message` VALUES (15, 2, 3, 'q', 1, '2025-12-15 09:43:17', 0, 0);
INSERT INTO `message` VALUES (16, 2, 3, 'qqq', 1, '2025-12-15 09:43:34', 0, 0);
INSERT INTO `message` VALUES (17, 2, 3, '11', 1, '2025-12-15 09:43:50', 0, 0);
INSERT INTO `message` VALUES (18, 2, 3, 'asda', 0, '2025-12-21 18:39:37', 0, 0);
INSERT INTO `message` VALUES (19, 2, 3, 'asd', 0, '2025-12-21 19:43:14', 0, 0);
INSERT INTO `message` VALUES (20, 1, 3, '1', 1, '2026-05-06 10:28:55', 0, 0);
INSERT INTO `message` VALUES (21, 2, 3, '1', 0, '2026-05-15 17:50:19', 0, 0);
INSERT INTO `message` VALUES (22, 2, 3, 'v', 0, '2026-05-15 18:00:35', 0, 0);
INSERT INTO `message` VALUES (23, 2, 1, '七', 1, '2026-05-15 18:02:42', 0, 0);
INSERT INTO `message` VALUES (24, 2, 1, 'q', 1, '2026-05-16 15:30:12', 0, 0);
INSERT INTO `message` VALUES (25, 2, 1, 'q\'w', 1, '2026-05-16 15:35:30', 0, 0);
INSERT INTO `message` VALUES (26, 2, 1, 'q\'w', 1, '2026-05-16 15:35:41', 0, 0);
INSERT INTO `message` VALUES (27, 2, 1, 'a', 1, '2026-05-16 15:38:18', 0, 0);
INSERT INTO `message` VALUES (28, 1, 2, '来', 1, '2026-05-16 15:38:49', 0, 0);
INSERT INTO `message` VALUES (29, 2, 1, 'a', 1, '2026-05-16 15:39:04', 0, 0);
INSERT INTO `message` VALUES (30, 2, 1, 'a', 1, '2026-05-16 15:39:26', 0, 0);
INSERT INTO `message` VALUES (31, 1, 2, 'a', 1, '2026-05-16 15:39:50', 0, 0);

-- ----------------------------
-- Table structure for resource
-- ----------------------------
DROP TABLE IF EXISTS `resource`;
CREATE TABLE `resource`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` bigint NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_id` bigint NULL DEFAULT NULL,
  `owner_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `like_count` int NULL DEFAULT 0,
  `download_count` int NULL DEFAULT 0,
  `view_count` int NULL DEFAULT 0,
  `allow_download` tinyint(1) NULL DEFAULT 1,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `visibility` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'VISIBLE',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_resource_category`(`category_id` ASC) USING BTREE,
  INDEX `fk_resource_owner`(`owner_id` ASC) USING BTREE,
  CONSTRAINT `fk_resource_category` FOREIGN KEY (`category_id`) REFERENCES `resource_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_resource_owner` FOREIGN KEY (`owner_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of resource
-- ----------------------------
INSERT INTO `resource` VALUES (4, '线性代数满分笔记', 1, '期中考试满分同学整理的课堂笔记，包含思维导图。', '/files/public/sample-linear-algebra.pdf', 2, 'student1', 21, 52, 240, 1, '2025-12-03 17:20:30', '2026-05-16 15:27:46', 'VISIBLE');
INSERT INTO `resource` VALUES (5, '操作系统实验报告模板', 2, '统一的实验报告格式模板，含评分标准说明。', '/files/public/sample-os-report.docx', 2, 'student1', 5, 18, 131, 1, '2025-12-03 17:20:30', '2026-05-16 15:27:47', 'VISIBLE');
INSERT INTO `resource` VALUES (7, 'oooo2', 2, 'sjdaskhca', '/files/public/6d2bb9bd-e075-4a3a-aa13-f265d4a6fdd3.doc', 3, 'student2', 3, 10, 103, 1, '2025-12-08 11:06:00', '2026-05-15 18:30:46', 'VISIBLE');
INSERT INTO `resource` VALUES (10, '未命名资料', 1, 'a', '', 3, 'student2', 2, 0, 89, 1, '2025-12-10 21:47:06', '2026-05-16 15:27:47', 'HIDDEN');
INSERT INTO `resource` VALUES (17, '合作开发协议书.doc', 4, NULL, '/files/public/ce395ff6-4f5e-41fa-8cb3-aa2176197570.doc', 3, 'student2', 1, 0, 8, 1, '2025-12-21 18:42:09', '2026-05-15 18:30:52', 'VISIBLE');
INSERT INTO `resource` VALUES (18, 'zz', 4, 'zz', '', 3, 'student2', 0, 0, 20, 1, '2025-12-21 18:43:05', '2026-05-16 15:27:54', 'VISIBLE');

-- ----------------------------
-- Table structure for resource_category
-- ----------------------------
DROP TABLE IF EXISTS `resource_category`;
CREATE TABLE `resource_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NULL DEFAULT 1,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of resource_category
-- ----------------------------
INSERT INTO `resource_category` VALUES (1, '课', 1, '2025-12-03 15:50:56', '2025-12-08 09:10:29');
INSERT INTO `resource_category` VALUES (2, '实验报告', 2, '2025-12-03 15:50:56', '2025-12-03 15:50:56');
INSERT INTO `resource_category` VALUES (3, '学习资料', 3, '2025-12-03 15:50:56', '2025-12-03 15:50:56');
INSERT INTO `resource_category` VALUES (4, 'qita', 1, '2025-12-10 21:59:39', '2025-12-10 21:59:39');

-- ----------------------------
-- Table structure for resource_comment
-- ----------------------------
DROP TABLE IF EXISTS `resource_comment`;
CREATE TABLE `resource_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_id` bigint NOT NULL,
  `root_id` bigint NULL DEFAULT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `is_anonymous` tinyint(1) NOT NULL DEFAULT 0,
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `reply_to_uid` bigint NULL DEFAULT NULL,
  `reply_to_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_comment_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_comment_resource_created`(`resource_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_comment_root`(`root_id` ASC) USING BTREE,
  INDEX `idx_comment_parent`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `fk_comment_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of resource_comment
-- ----------------------------
INSERT INTO `resource_comment` VALUES (1, 7, 1, NULL, 2, 1, '已删除', '', NULL, NULL, '该评论已删除', 'DELETED', '2025-12-12 15:27:51', '2025-12-21 19:20:47');
INSERT INTO `resource_comment` VALUES (2, 4, 2, NULL, 2, 0, '1', '', NULL, NULL, 'qw', 'NORMAL', '2025-12-14 17:11:06', '2025-12-14 17:11:06');
INSERT INTO `resource_comment` VALUES (11, 10, 11, NULL, 1, 0, '管理员1', '/files/public/f8aa24e4-494c-44b8-9d79-66a235227e4e.png', NULL, NULL, 'q', 'NORMAL', '2025-12-14 19:27:30', '2025-12-14 19:27:30');
INSERT INTO `resource_comment` VALUES (15, 7, 15, NULL, 2, 0, '1', '/files/public/71b82fb7-369f-4410-83c8-ed2e9355403a.jpg', NULL, NULL, 'hhh', 'NORMAL', '2025-12-21 19:20:44', '2025-12-21 19:20:44');
INSERT INTO `resource_comment` VALUES (16, 7, 15, 15, 3, 1, '已删除', '/files/public/f4e339cc-d996-4be3-a382-64812dec086f.jpg', 2, '1', '该评论已删除', 'DELETED', '2025-12-21 19:21:10', '2025-12-21 19:21:17');
INSERT INTO `resource_comment` VALUES (17, 7, 17, NULL, 3, 1, '已删除', '/files/public/f4e339cc-d996-4be3-a382-64812dec086f.jpg', NULL, NULL, '该评论已删除', 'DELETED', '2025-12-21 19:21:21', '2025-12-21 19:21:29');
INSERT INTO `resource_comment` VALUES (18, 7, 15, 15, 3, 0, 'ooo', '/files/public/f4e339cc-d996-4be3-a382-64812dec086f.jpg', 2, '1', 'oo', 'NORMAL', '2025-12-21 19:21:28', '2025-12-21 19:21:28');

-- ----------------------------
-- Table structure for resource_favorite
-- ----------------------------
DROP TABLE IF EXISTS `resource_favorite`;
CREATE TABLE `resource_favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_fav_user_resource`(`resource_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `fk_fav_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_fav_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 95 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of resource_favorite
-- ----------------------------
INSERT INTO `resource_favorite` VALUES (21, 7, 4, '2025-12-08 14:56:45');
INSERT INTO `resource_favorite` VALUES (50, 5, 2, '2025-12-10 14:44:36');
INSERT INTO `resource_favorite` VALUES (59, 7, 3, '2025-12-10 21:51:02');
INSERT INTO `resource_favorite` VALUES (88, 10, 2, '2025-12-14 14:26:29');
INSERT INTO `resource_favorite` VALUES (92, 10, 1, '2026-05-15 17:48:40');
INSERT INTO `resource_favorite` VALUES (94, 18, 2, '2026-05-16 15:27:58');

-- ----------------------------
-- Table structure for resource_like
-- ----------------------------
DROP TABLE IF EXISTS `resource_like`;
CREATE TABLE `resource_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_like_user_resource`(`resource_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `fk_like_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_like_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 76 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of resource_like
-- ----------------------------
INSERT INTO `resource_like` VALUES (26, 7, 4, '2025-12-08 15:34:10');
INSERT INTO `resource_like` VALUES (67, 10, 2, '2025-12-14 14:26:30');
INSERT INTO `resource_like` VALUES (71, 4, 3, '2025-12-14 18:10:48');
INSERT INTO `resource_like` VALUES (72, 10, 1, '2025-12-14 23:58:58');
INSERT INTO `resource_like` VALUES (75, 17, 2, '2026-05-15 17:49:52');

-- ----------------------------
-- Table structure for resource_report
-- ----------------------------
DROP TABLE IF EXISTS `resource_report`;
CREATE TABLE `resource_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING',
  `review_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` datetime NULL DEFAULT NULL,
  `cancelled` tinyint(1) NULL DEFAULT 0 COMMENT '是否已撤销',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `resource_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_report_resource`(`resource_id` ASC) USING BTREE,
  INDEX `idx_report_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_report_cancelled`(`cancelled` ASC) USING BTREE,
  CONSTRAINT `fk_report_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of resource_report
-- ----------------------------
INSERT INTO `resource_report` VALUES (1, 7, 2, 'h', 'RESOLVED', 'w', '2025-12-10 14:57:01', '2025-12-10 15:03:53', 0, '2025-12-10 15:03:52', NULL);
INSERT INTO `resource_report` VALUES (3, 7, 3, 'a', 'RESOLVED', 'h', '2025-12-10 16:28:26', '2025-12-21 19:49:15', 0, '2025-12-21 19:49:14', 'oooo2');
INSERT INTO `resource_report` VALUES (4, 7, 3, 'as', 'RESOLVED', '违规内容已隐藏', '2025-12-10 17:37:00', '2025-12-10 17:37:25', 0, '2025-12-10 17:37:25', 'oooo2');
INSERT INTO `resource_report` VALUES (5, 4, 3, 'aaaaa', 'RESOLVED', 'aaaaaa', '2025-12-10 17:38:47', '2025-12-10 17:39:30', 0, '2025-12-10 17:39:30', '线性代数满分笔记');
INSERT INTO `resource_report` VALUES (7, 7, 2, 'zzzz', 'RESOLVED', 'asas', '2025-12-10 17:46:42', '2025-12-10 17:47:08', 0, '2025-12-10 17:47:07', 'oooo2');
INSERT INTO `resource_report` VALUES (9, 7, 2, 'ssss', 'RESOLVED', 'ss', '2025-12-10 17:52:26', '2025-12-10 17:53:15', 0, '2025-12-10 17:53:14', 'oooo2');
INSERT INTO `resource_report` VALUES (11, 7, 2, 'zzz', 'PENDING', '违规内容已隐藏', '2025-12-10 18:01:35', '2025-12-10 18:01:46', 0, '2025-12-15 09:04:24', 'oooo2');
INSERT INTO `resource_report` VALUES (12, 4, 3, 'A', 'CANCELLED', '已处理', '2025-12-10 20:41:40', '2025-12-21 19:50:42', 1, '2025-12-21 19:51:05', '线性代数满分笔记');
INSERT INTO `resource_report` VALUES (13, 5, 3, 'Z', 'RESOLVED', 'A', '2025-12-10 20:43:09', '2025-12-10 21:13:27', 0, '2025-12-10 21:13:26', '操作系统实验报告模板');
INSERT INTO `resource_report` VALUES (14, 10, 2, '222', 'RESOLVED', 'o', '2025-12-14 20:03:31', '2025-12-14 20:04:13', 0, '2025-12-14 20:04:13', '未命名资料');
INSERT INTO `resource_report` VALUES (16, 10, 1, '管理员直接隐藏资料', 'RESOLVED', '管理员直接隐藏资料', '2025-12-14 23:36:46', '2025-12-14 23:36:47', 0, '2025-12-14 23:36:46', '未命名资料');
INSERT INTO `resource_report` VALUES (22, 18, 2, '虚假信息', 'PENDING', NULL, '2025-12-21 20:07:24', NULL, 0, '2025-12-21 20:07:24', 'zz');
INSERT INTO `resource_report` VALUES (23, 17, 3, '21', 'PENDING', NULL, '2026-05-15 18:31:03', NULL, 0, '2026-05-15 18:31:03', '合作开发协议书.doc');
INSERT INTO `resource_report` VALUES (24, 18, 2, '90', 'PENDING', NULL, '2026-05-15 18:36:55', NULL, 0, '2026-05-15 18:36:55', 'zz');
INSERT INTO `resource_report` VALUES (25, 18, 2, '】', 'REJECTED', '已驳回', '2026-05-16 15:28:06', '2026-05-16 15:29:18', 0, '2026-05-16 15:29:17', 'zz');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STUDENT',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$qJEpS3EWgSxwf6/CR9oTJueX6jRcPTUDg3dG0n6HULyNonVy19ZTi', 'ADMIN', '管理员1', '/files/public/f8aa24e4-494c-44b8-9d79-66a235227e4e.png', 'FEMALE', '', '', '2025-12-03 15:50:56', '2026-05-16 16:20:43', '2026-05-16 16:20:43');
INSERT INTO `sys_user` VALUES (2, 'student1', '/YB9jqphYSKjeFlb.sTAlZZYvkmZwAfuKZC/9b0YAu6u', 'STUDENT', '1', '/files/public/71b82fb7-369f-4410-83c8-ed2e9355403a.jpg', 'FEMALE', '', 'y12251122@126.com', '2025-12-03 15:50:56', '2026-05-16 15:38:01', '2026-05-16 15:38:01');
INSERT INTO `sys_user` VALUES (3, 'student2', '$2a$10$ZXWT496ZsydZhx0tkxxLdu9pV9FQmgo0uK1Z8aMWg/.udcwiFIxCS', 'STUDENT', 'ooo', '/files/public/f4e339cc-d996-4be3-a382-64812dec086f.jpg', '', '', '', '2025-12-08 10:24:08', '2026-05-15 18:16:37', '2026-05-15 18:16:38');
INSERT INTO `sys_user` VALUES (4, 'student3', '$2a$10$Zck1QjXr.q7JK9KmabHoc.0lF/ZipDWKrrzxAAK4hn08UyPRdgqxW', 'STUDENT', 'D', '', '', '', '', '2025-12-08 14:50:31', '2025-12-21 19:37:11', '2025-12-21 19:36:14');

-- ----------------------------
-- Table structure for user_friend
-- ----------------------------
DROP TABLE IF EXISTS `user_friend`;
CREATE TABLE `user_friend`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `friend_id` bigint NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FRIEND',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_friend`(`user_id` ASC, `friend_id` ASC) USING BTREE,
  INDEX `fk_uf_friend`(`friend_id` ASC) USING BTREE,
  CONSTRAINT `fk_uf_friend` FOREIGN KEY (`friend_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_uf_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_friend
-- ----------------------------
INSERT INTO `user_friend` VALUES (29, 1, 3, 'FRIEND', '2025-12-15 08:27:57', '2025-12-15 08:27:57');
INSERT INTO `user_friend` VALUES (30, 3, 1, 'FRIEND', '2025-12-15 08:27:57', '2025-12-15 08:27:57');
INSERT INTO `user_friend` VALUES (31, 3, 4, 'FRIEND', '2025-12-21 19:36:26', '2025-12-21 19:37:59');
INSERT INTO `user_friend` VALUES (32, 4, 3, 'FRIEND', '2025-12-21 19:36:26', '2025-12-21 19:36:26');
INSERT INTO `user_friend` VALUES (33, 4, 1, 'FRIEND', '2025-12-21 20:56:39', '2025-12-21 20:56:39');
INSERT INTO `user_friend` VALUES (34, 1, 4, 'FRIEND', '2025-12-21 20:56:39', '2025-12-21 20:56:39');
INSERT INTO `user_friend` VALUES (37, 2, 1, 'FRIEND', '2026-05-16 15:30:00', '2026-05-16 15:30:00');
INSERT INTO `user_friend` VALUES (38, 1, 2, 'FRIEND', '2026-05-16 15:30:00', '2026-05-16 15:30:00');

-- ----------------------------
-- Table structure for user_punishment
-- ----------------------------
DROP TABLE IF EXISTS `user_punishment`;
CREATE TABLE `user_punishment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '被处罚用户ID',
  `report_id` bigint NULL DEFAULT NULL COMMENT '关联的举报ID（资料举报）',
  `punishment_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '处罚类型：WARNING-警告，SUSPENSION-禁止上传资料，MUTE-禁言，BAN-封禁',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '处罚原因',
  `duration` int NULL DEFAULT NULL COMMENT '处罚时长（天），NULL表示永久',
  `start_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处罚开始时间',
  `end_date` datetime NULL DEFAULT NULL COMMENT '处罚结束时间，NULL表示永久',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-生效中，REVOKED-已撤销，EXPIRED-已过期',
  `admin_id` bigint NULL DEFAULT NULL COMMENT '执行处罚的管理员ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resource_id` bigint NULL DEFAULT NULL COMMENT '关联的资料ID（资料处罚）',
  `resource_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资料标题（冗余字段）',
  `reported_user_id` bigint NULL DEFAULT NULL COMMENT '被举报的用户ID（用户举报导致的处罚）',
  `reported_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '被举报用户名（冗余字段）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_punishment_admin`(`admin_id` ASC) USING BTREE,
  INDEX `idx_punishment_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_punishment_report`(`report_id` ASC) USING BTREE,
  CONSTRAINT `fk_punishment_admin` FOREIGN KEY (`admin_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_punishment_report` FOREIGN KEY (`report_id`) REFERENCES `resource_report` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_punishment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户处罚信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_punishment
-- ----------------------------
INSERT INTO `user_punishment` VALUES (1, 2, 5, 'WARNING', 'aaa', NULL, '2025-12-10 17:39:30', NULL, 'ACTIVE', 1, '2025-12-10 17:39:30', '2025-12-10 17:39:30', NULL, NULL, NULL, NULL);
INSERT INTO `user_punishment` VALUES (10, 2, 13, 'SUSPENSION', 'ad', 2, '2025-12-10 21:13:27', '2025-12-12 21:13:27', 'ACTIVE', 1, '2025-12-10 21:13:26', '2025-12-10 21:13:26', 5, '操作系统实验报告模板', NULL, NULL);
INSERT INTO `user_punishment` VALUES (11, 3, 14, 'WARNING', '违规内容已隐藏', NULL, '2025-12-14 20:04:13', NULL, 'ACTIVE', 1, '2025-12-14 20:04:13', '2025-12-14 20:04:13', 10, '未命名资料', NULL, NULL);
INSERT INTO `user_punishment` VALUES (13, 3, NULL, 'WARNING', 'ee', 1, '2025-12-14 21:37:28', '2025-12-15 21:37:28', 'ACTIVE', 1, '2025-12-14 21:37:28', '2025-12-14 21:37:28', 10, '未命名资料', NULL, NULL);
INSERT INTO `user_punishment` VALUES (14, 3, NULL, 'WARNING', 'ggg', 1, '2025-12-14 21:45:09', '2025-12-15 21:45:09', 'ACTIVE', 1, '2025-12-14 21:45:09', '2025-12-14 21:45:09', 9, 'ad', NULL, NULL);
INSERT INTO `user_punishment` VALUES (15, 4, NULL, 'WARNING', '管理员直接处罚', 1, '2025-12-14 22:03:27', '2025-12-15 22:03:27', 'ACTIVE', 1, '2025-12-14 22:03:27', '2025-12-14 22:03:27', NULL, NULL, NULL, NULL);
INSERT INTO `user_punishment` VALUES (16, 3, NULL, 'WARNING', '管理员直接处罚', 1, '2025-12-15 00:49:47', '2025-12-16 00:49:47', 'ACTIVE', 1, '2025-12-15 00:49:46', '2025-12-15 00:49:46', NULL, NULL, NULL, NULL);
INSERT INTO `user_punishment` VALUES (18, 4, NULL, 'MUTE', '管理员直接处罚', 1, '2025-12-15 08:23:44', '2025-12-16 08:23:44', 'ACTIVE', 1, '2025-12-15 08:23:44', '2025-12-15 08:23:44', NULL, NULL, NULL, NULL);
INSERT INTO `user_punishment` VALUES (19, 3, 4, 'WARNING', '', 1, '2025-12-15 09:42:57', '2025-12-16 09:42:57', 'ACTIVE', 1, '2025-12-15 09:42:57', '2025-12-15 09:42:57', NULL, NULL, 3, 'student2');
INSERT INTO `user_punishment` VALUES (20, 3, NULL, 'MUTE', 'dddddddddd', 1, '2025-12-15 11:36:54', '2025-12-16 11:36:54', 'ACTIVE', 1, '2025-12-15 11:36:54', '2025-12-15 11:36:54', NULL, NULL, NULL, NULL);
INSERT INTO `user_punishment` VALUES (21, 4, 7, 'MUTE', '发布违规言论', 1, '2025-12-21 19:53:26', '2025-12-22 19:53:26', 'ACTIVE', 1, '2025-12-21 19:53:26', '2025-12-21 19:53:26', NULL, NULL, 4, 'student3');
INSERT INTO `user_punishment` VALUES (22, 2, NULL, 'WARNING', '管理员直接处罚', NULL, '2026-05-16 15:31:59', NULL, 'ACTIVE', 1, '2026-05-16 15:31:58', '2026-05-16 15:31:58', NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for user_report
-- ----------------------------
DROP TABLE IF EXISTS `user_report`;
CREATE TABLE `user_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reported_user_id` bigint NOT NULL COMMENT '被举报的用户ID',
  `user_id` bigint NOT NULL COMMENT '举报人ID',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报原因',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING' COMMENT '举报状态：PENDING(待处理)、RESOLVED(已处理)、REJECTED(已驳回)、CANCELLED(已撤销)',
  `review_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审核回复',
  `reported_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '被举报用户名（冗余字段，用于显示）',
  `cancelled` tinyint(1) NULL DEFAULT 0 COMMENT '是否已取消',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_report_reported_user`(`reported_user_id` ASC) USING BTREE,
  INDEX `idx_user_report_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_report_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_user_report_reported_user` FOREIGN KEY (`reported_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_report_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_report
-- ----------------------------
INSERT INTO `user_report` VALUES (1, 3, 2, '111', 'PENDING', 'wda', 'student2', 0, '2025-12-14 20:03:19', '2025-12-14 20:08:33', '2025-12-14 20:26:17');
INSERT INTO `user_report` VALUES (2, 4, 1, '管理员直接处罚', 'RESOLVED', '管理员直接处罚：管理员直接处罚', 'student3', 0, '2025-12-14 22:03:27', '2025-12-14 22:03:27', '2025-12-14 22:03:27');
INSERT INTO `user_report` VALUES (3, 3, 1, '管理员直接处罚', 'RESOLVED', '管理员直接处罚：管理员直接处罚', 'student2', 0, '2025-12-15 00:49:46', '2025-12-15 00:49:47', '2025-12-15 00:49:46');
INSERT INTO `user_report` VALUES (4, 3, 2, '11', 'RESOLVED', '111', 'student2', 0, '2025-12-15 00:56:21', '2025-12-15 09:42:57', '2025-12-15 09:42:57');
INSERT INTO `user_report` VALUES (5, 4, 1, '管理员直接处罚', 'RESOLVED', '管理员直接处罚：管理员直接处罚', 'student3', 0, '2025-12-15 08:23:44', '2025-12-15 08:23:44', '2025-12-15 08:23:44');
INSERT INTO `user_report` VALUES (6, 3, 1, 'dddddddddd', 'RESOLVED', '管理员直接处罚：dddddddddd', 'student2', 0, '2025-12-15 11:36:54', '2025-12-15 11:36:54', '2025-12-15 11:36:54');
INSERT INTO `user_report` VALUES (7, 4, 3, 'uuu', 'RESOLVED', '已处罚禁言一天', 'student3', 0, '2025-12-21 19:52:18', '2025-12-21 19:53:26', '2025-12-21 19:53:26');
INSERT INTO `user_report` VALUES (8, 3, 2, 'z', 'PENDING', NULL, 'student2', 0, '2025-12-21 20:30:02', NULL, '2025-12-21 20:30:02');
INSERT INTO `user_report` VALUES (9, 2, 1, '管理员直接处罚', 'RESOLVED', '管理员直接处罚：管理员直接处罚', 'student1', 0, '2026-05-16 15:31:58', '2026-05-16 15:31:59', '2026-05-16 15:31:58');

SET FOREIGN_KEY_CHECKS = 1;
