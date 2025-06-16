ALTER TABLE `jinbooks`.`jbx_employee_salary_temp` 
ADD COLUMN `pay_amount` DECIMAL(10,5) NULL DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣' AFTER `personal_tax`;


ALTER TABLE `jinbooks`.`jbx_employee_salary` 
ADD COLUMN `pay_amount` DECIMAL(10,5) NULL DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣' AFTER `personal_tax`;

ALTER TABLE `jinbooks`.`jbx_employee_salary_summary` 
ADD COLUMN `pay_amount` DECIMAL(10,5) NULL DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣' AFTER `personal_tax`;

ALTER TABLE `jinbooks`.`jbx_employee_salary_summary` 
DROP COLUMN `salary_voucher_id`,
DROP COLUMN `accrual_voucher_id`;