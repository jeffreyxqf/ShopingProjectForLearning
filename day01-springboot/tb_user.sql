/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 50723
 Source Host           : localhost:3306
 Source Schema         : yun6

 Target Server Type    : MySQL
 Target Server Version : 50723
 File Encoding         : 65001

 Date: 08/09/2018 16:33:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `name` varchar(100) DEFAULT NULL COMMENT '姓名',
  `age` int(10) DEFAULT NULL COMMENT '年龄',
  `sex` tinyint(1) DEFAULT NULL COMMENT '性别，1男性，2女性',
  `birthday` date DEFAULT NULL COMMENT '出生日期',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `created` datetime DEFAULT NULL COMMENT '创建时间',
  `updated` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`user_name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of tb_user
-- ----------------------------
BEGIN;
INSERT INTO `tb_user` VALUES (1, 'zhangsan', '123456', '张三', 30, 1, '1964-08-08', '张三同学在学Java', '2014-09-19 16:56:04', '2014-09-21 11:24:59');
INSERT INTO `tb_user` VALUES (2, 'lisi', '123456', '李四', 21, 2, '1995-01-01', '李四同学在传智学Java', '2014-09-19 16:56:04', '2014-09-19 16:56:04');
INSERT INTO `tb_user` VALUES (3, 'wangwu', '123456', '王五', 22, 2, '1994-01-01', '王五同学在学php', '2014-09-19 16:56:04', '2014-09-19 16:56:04');
INSERT INTO `tb_user` VALUES (4, 'zhangwei', '123456', '张伟', 20, 1, '1996-09-01', '张伟同学在传智播客学Java', '2014-09-19 16:56:04', '2014-09-19 16:56:04');
INSERT INTO `tb_user` VALUES (5, 'lina', '123456', '李娜', 28, 1, '1988-01-01', '李娜同学在传智播客学Java', '2014-09-19 16:56:04', '2014-09-19 16:56:04');
INSERT INTO `tb_user` VALUES (6, 'lilei', '123456', '李磊', 23, 1, '1993-08-08', '李磊同学在传智播客学Java', '2014-09-20 11:41:15', '2014-09-20 11:41:15');
INSERT INTO `tb_user` VALUES (7, 'hanmeimei', '123456', '韩梅梅', 24, 2, '1992-08-08', '韩梅梅同学在传智播客学php', '2014-09-20 11:41:15', '2014-09-20 11:41:15');
INSERT INTO `tb_user` VALUES (8, 'liuyan', '123456', '柳岩', 21, 2, '1995-08-08', '柳岩同学在传智播客学表演', '2014-09-20 11:41:15', '2014-09-20 11:41:15');
INSERT INTO `tb_user` VALUES (9, 'liuyifei', '123456', '刘亦菲', 18, 2, '1998-08-08', '刘亦菲同学在传智播客学唱歌', '2014-09-20 11:41:15', '2014-09-20 11:41:15');
INSERT INTO `tb_user` VALUES (10, 'fanbingbing', '123456', '范冰冰', 25, 2, '1991-08-08', '范冰冰同学在传智播客学表演', '2014-09-20 11:41:15', '2014-09-20 11:41:15');
INSERT INTO `tb_user` VALUES (11, 'zhengshuang', '123456', '郑爽', 23, 2, '1993-08-08', '郑爽同学在传智播客学习如何装纯', '2014-09-20 11:41:15', '2014-09-20 11:41:15');
INSERT INTO `tb_user` VALUES (12, 'tangyan', '123456', '唐嫣', 26, 2, '1990-08-08', '郑爽同学在传智播客学习如何耍酷', '2014-09-20 11:41:15', '2014-09-20 11:41:15');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
