-- Adds Contract.isReadyForSign, required by ContractService.markReadyForSign()
-- and the PUT /contract/{uuid}/markReadyForSign endpoint.
-- Upstream (membership-ms-main, 2026-09-03) shipped the entity field without a
-- migration; ddl-auto is `none`, so the column must be added explicitly.
ALTER TABLE `contract`
  ADD COLUMN `isReadyForSign` bit(1) DEFAULT NULL AFTER `zipcode`;

-- Backfill: organizers only see contracts with isReadyForSign = 1
-- (ContractServiceImpl list filter). Without this, every pre-existing contract
-- disappears from the organizer's list, including already-signed ones.
-- Contracts that already have a document, a signature, or an uploaded copy were
-- visible to the organizer before this change, so keep them visible.
UPDATE `contract`
   SET `isReadyForSign` = 1
 WHERE `isReadyForSign` IS NULL
   AND ((`contractPath` IS NOT NULL AND `contractPath` <> '')
        OR `customerSignature` IS NOT NULL
        OR `isUploadContract` = 1);
