-- Add to this file the database changes that need to be applied to the %playapps database
-- Reset this page after each release and after having created a SVN tag
--

ALTER TABLE `Insight` ADD COLUMN `featuredHome` BIT(1)  NOT NULL;



