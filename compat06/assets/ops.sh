#!/system/bin/sh
# Invoked inside the current system_server mount namespace; arguments are validated in app and here.
set -eu
action="$1"
feature="$2"
case "$feature" in
 clone) T=/system/emui/base/xml/hw_clone_app_list.xml ;;
 parallel) T=/hw_product/region_comm/china/xml/magic_window_application_list.xml ;;
 *) echo BAD_FEATURE; exit 30 ;;
esac
[ "$(id -u)" = 0 ] && [ "$(getprop ro.product.model)" = DBY2-W00 ] && [ "$(getprop ro.build.display.id)" = 'DBY2-W00 4.2.0.252(C00E100R3P5)' ] || { echo DEVICE_MISMATCH; exit 31; }
p=$(pidof system_server)
case "$p" in ''|*[!0-9]*) echo SYSTEM_SERVER_UNKNOWN; exit 32;; esac
ns=$(readlink /proc/self/ns/mnt)
[ "$ns" = "$(readlink /proc/$p/ns/mnt)" ] || { echo NAMESPACE_MISMATCH; exit 33; }
[ -f "$T" ] || { echo XML_MISSING; exit 34; }
count() { awk -v t="$T" '$5==t {n++} END{print n+0}' /proc/self/mountinfo; }
hash() { sha256sum "$1" | awk '{print $1}'; }
boot=$(cat /proc/sys/kernel/random/boot_id)
case "$boot" in ''|*[!0-9a-f-]*) echo BOOT_ID_UNKNOWN; exit 35;; esac
D=/data/adb/air2023_compat06/$boot/$feature
S=$D/generated.xml
guard() {
  found=0
  for z in $(pidof zygote64 2>/dev/null || true) $(pidof zygote 2>/dev/null || true); do
    found=1
    [ "$(readlink /proc/$z/ns/mnt)" = "$ns" ] || { echo ZYGOTE_NAMESPACE_DIFFERS; return 1; }
  done
  [ "$found" = 1 ] || { echo ZYGOTE_NOT_FOUND; return 1; }
  if [ "$feature" = clone ]; then
    for z in $(pidof com.huawei.systemmanager 2>/dev/null || true); do
      [ "$(readlink /proc/$z/ns/mnt)" = "$ns" ] || { echo SYSTEMMANAGER_NAMESPACE_DIFFERS; return 1; }
    done
  fi
}
own() {
  [ "$(count)" = 1 ] && [ -f "$S" ] && [ -f "$D/original.sha" ] && [ -f "$D/namespace" ] &&
  [ "$(cat "$D/namespace")" = "$ns" ] &&
  [ "$(stat -c '%d:%i' "$S")" = "$(stat -c '%d:%i' "$T")" ]
}
case "$action" in
 status)
   echo feature="$feature"; echo target="$T"; echo system_server_pid="$p"; echo namespace="$ns"; echo boot="$boot"; echo exact_mounts="$(count)"; echo sha256="$(hash "$T")"
   if guard; then echo SHARED_NAMESPACE_OK; else echo APPLY_BLOCKED_NAMESPACE; fi
   if own; then echo OWN_MOUNT; fi
   if [ -d "$D" ]; then echo TRANSACTION_DIR_EXISTS; fi
   echo END ;;
 source)
   [ "$(count)" = 0 ] || { echo EXISTING_MOUNT; exit 36; }
   cat "$T" ;;
 packages)
   pm list packages -3 --user 0 ;;
 apply)
   expected="$3"; generated="$4"; expectedns="$5"
   [ "$ns" = "$expectedns" ] || { echo STALE_NAMESPACE; exit 37; }
   guard || exit 38
   [ "$(count)" = 0 ] && [ "$(hash "$T")" = "$expected" ] || { echo BASELINE_CHANGED; exit 39; }
   case "$expected$generated" in *[!0-9a-f]*) exit 40;; esac
   [ "${#expected}" = 64 ] && [ "${#generated}" = 64 ] || exit 40
   label=$(ls -Zd "$T" | awk '{print $1}')
   case "$label" in u:object_r:*:s0) ;; *) echo UNKNOWN_FILE_CONTEXT; exit 41;; esac
   umask 077
   [ ! -L /data/adb/air2023_compat06 ] || exit 42
   mkdir -p "/data/adb/air2023_compat06/$boot"
   [ ! -L "/data/adb/air2023_compat06/$boot" ] || exit 42
   mkdir "$D" || { echo EXISTING_TRANSACTION_RESTORE_FIRST; exit 43; }
   printf '%s\n' "$expected" > "$D/original.sha"
   printf '%s\n' "$ns" > "$D/namespace"
   cat > "$S"
   [ "$(hash "$S")" = "$generated" ] || { echo INPUT_HASH_MISMATCH; exit 44; }
   chmod 0644 "$S"; chown 0:0 "$S"; chcon "$label" "$S" || { echo LABEL_FAILED; exit 45; }
   [ "$(count)" = 0 ] && [ "$(hash "$T")" = "$expected" ] || { echo BASELINE_CHANGED; exit 39; }
   mount -o bind "$S" "$T" || { echo BIND_FAILED; exit 46; }
   if ! own || [ "$(hash "$T")" != "$generated" ] || ! guard; then
      echo VERIFY_FAILED
      if own; then umount "$T" || echo ROLLBACK_FAILED; fi
      exit 47
   fi
   echo MOUNT_VERIFIED; echo sha256="$(hash "$T")"; echo namespace="$ns"; echo END ;;
 restore)
   if [ "$(count)" = 0 ] && [ -d "$D" ] && [ -f "$D/namespace" ] && [ "$(cat "$D/namespace")" = "$ns" ] && [ -f "$D/original.sha" ] && [ "$(hash "$T")" = "$(cat "$D/original.sha")" ]; then
      rm -f "$S" "$D/original.sha" "$D/namespace"; rmdir "$D"; echo ORIGINAL_ALREADY_VISIBLE; echo END; exit 0
   fi
   own || { echo NOT_OWN_MOUNT_REFUSE; exit 48; }
   original=$(cat "$D/original.sha")
   umount "$T" || { echo UNMOUNT_FAILED; exit 49; }
   [ "$(count)" = 0 ] && [ "$(hash "$T")" = "$original" ] || { echo ORIGINAL_VERIFY_FAILED; exit 50; }
   rm -f "$S" "$D/original.sha" "$D/namespace"; rmdir "$D"
   echo ORIGINAL_RESTORED; echo END ;;
 *) exit 51 ;;
esac
