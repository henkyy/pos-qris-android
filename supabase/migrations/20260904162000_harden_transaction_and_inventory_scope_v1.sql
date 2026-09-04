create or replace function public.purchase_order_confirm(p_purchase_order_id uuid)
returns table(purchase_order_id uuid,status text)
language plpgsql security definer set search_path=public,private
as $function$
declare v_business uuid; v_branch uuid; v_status text;
begin
 if auth.uid() is null then raise exception 'Authentication required.'; end if;
 select po.business_id,po.branch_id,po.status::text into v_business,v_branch,v_status from public.purchase_orders po where po.id=p_purchase_order_id for update;
 if v_business is null then raise exception 'Purchase order tidak ditemukan.'; end if;
 if not exists(select 1 from private.current_business_ids() x where x=v_business) then raise exception 'Akses bisnis ditolak.'; end if;
 if not exists(select 1 from public.branches b where b.id=v_branch and b.business_id=v_business and b.is_active=true) then raise exception 'Cabang tidak valid untuk bisnis ini.'; end if;
 if not exists(select 1 from private.current_branch_ids() x where x=v_branch) then raise exception 'Akses cabang ditolak.'; end if;
 if v_status<>'DRAFT' then raise exception 'Hanya PO DRAFT yang dapat dikonfirmasi.'; end if;
 update public.purchase_orders po set status='CONFIRMED'::public.document_status,updated_at=now() where po.id=p_purchase_order_id;
 return query select p_purchase_order_id,'CONFIRMED'::text;
end;
$function$;

create or replace function public.payable_register_payment(p_payable_id uuid,p_amount bigint,p_reference text default null,p_notes text default null)
returns table(payable_id uuid,paid_amount bigint,outstanding_amount bigint,status text)
language plpgsql security definer set search_path=public,private
as $function$
declare v_payable public.payables%rowtype; v_new_paid bigint; v_out bigint; v_status text;
begin
 if auth.uid() is null then raise exception 'Authentication required.'; end if;
 if p_amount<=0 then raise exception 'Jumlah pembayaran harus lebih dari 0.'; end if;
 select * into v_payable from public.payables where id=p_payable_id for update;
 if v_payable.id is null then raise exception 'Hutang tidak ditemukan.'; end if;
 if not exists(select 1 from private.current_business_ids() x where x=v_payable.business_id) then raise exception 'Akses bisnis ditolak.'; end if;
 if not exists(select 1 from public.branches b where b.id=v_payable.branch_id and b.business_id=v_payable.business_id and b.is_active=true) then raise exception 'Cabang tidak valid untuk bisnis ini.'; end if;
 if not exists(select 1 from private.current_branch_ids() x where x=v_payable.branch_id) then raise exception 'Akses cabang ditolak.'; end if;
 if p_amount>v_payable.outstanding_amount then raise exception 'Pembayaran melebihi saldo hutang.'; end if;
 v_new_paid:=v_payable.paid_amount+p_amount; v_out:=v_payable.original_amount-v_new_paid; v_status:=case when v_out=0 then 'PAID' else 'OPEN' end;
 insert into public.payable_payments(payable_id,amount,paid_at,reference,notes) values(p_payable_id,p_amount,now(),nullif(trim(p_reference),''),nullif(trim(p_notes),''));
 update public.payables set paid_amount=v_new_paid,outstanding_amount=v_out,status=v_status where id=p_payable_id;
 return query select p_payable_id,v_new_paid,v_out,v_status;
end;
$function$;

drop policy if exists stock_movements_member_read on public.stock_movements;
create policy stock_movements_member_read on public.stock_movements for select to authenticated using (
 business_id in (select private.current_business_ids())
 and branch_id in (select private.current_branch_ids())
 and exists (select 1 from public.branches b where b.id=stock_movements.branch_id and b.business_id=stock_movements.business_id)
);

drop policy if exists stock_balances_branch_read on public.stock_balances;
create policy stock_balances_branch_read on public.stock_balances for select to authenticated using (
 exists (
  select 1 from public.locations l
  join public.branches b on b.id=l.branch_id
  where l.id=stock_balances.location_id
   and l.is_active=true
   and b.is_active=true
   and l.branch_id in (select private.current_branch_ids())
   and b.business_id in (select private.current_business_ids())
 )
);