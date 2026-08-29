import hashlib

def generate_valid_code(device_id: str):
    # 這裡的 SALT 必須與 Android 代碼中的 DataManagementUtils.SALT 保持一致
    salt = "SSWU_SALT_2026"
    input_str = device_id + salt
    hash_obj = hashlib.md5(input_str.encode())
    hex_str = hash_obj.hexdigest().upper()
    
    # 生成規則：SSWU-前8位-後4位
    return f"SSWU-{hex_str[:8]}-{hex_str[-4:]}"

if __name__ == "__main__":
    import sys
    if len(sys.argv) < 2:
        print("-" * 40)
        print("SSWU 工具箱激活碼生成器")
        print("-" * 40)
        print("使用方法: python generate_activation_code.py <設備_ID>")
        print("-" * 40)
        sys.exit(1)
    
    dev_id = sys.argv[1]
    code = generate_valid_code(dev_id)
    print("\n" + "=" * 40)
    print(f"設備 ID: {dev_id}")
    print(f"激活碼:  {code}")
    print("=" * 40 + "\n")
