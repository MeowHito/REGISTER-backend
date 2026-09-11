-- Adds Contract.isReadyForSign, required by ContractService.markReadyForSign()
-- and the PUT /contract/{uuid}/markReadyForSign endpoint.
-- Upstream (membership-ms-main, 2026-09-03) shipped the entity field without a
-- migration; ddl-auto is `none`, so the column must be added explicitly.
ALTER TABLE `contract`
  ADD COLUMN `isReadyForSign` bit(1) DEFAULT NULL AFTER `zipcode`;

-- Existing signed/completed contracts keep the default NULL (treated as draft).
